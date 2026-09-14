package com.pawtrail.pet.application.service;

import com.pawtrail.pet.application.support.AfterCommitExecutor;
import com.pawtrail.pet.domain.model.Pet;
import com.pawtrail.pet.domain.provider.StorageProvider;
import com.pawtrail.pet.domain.repository.PetRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 탈퇴한 계정에 대해 이 서비스가 가진 것을 정리합니다.
 *
 * account.withdrawn 을 받는 경로에서만 불립니다. 사용자가 직접 부르는 API 는 없습니다.
 * 탈퇴 자체는 auth 가 처리하고 이쪽은 그 결과를 받아 자기 몫을 지웁니다.
 *
 * PetService 에 넣지 않은 이유
 *
 * 그 클래스는 반려동물 하나를 보고 고치는 일을 모아 둔 곳이고 이미 여섯을 주입받고 있습니다.
 * 탈퇴는 표와 객체 저장소를 가로지르는 동작이고, 부르는 경로도 사용자 요청이 아니라 이벤트입니다.
 * user 와 auth 도 같은 이유로 탈퇴를 별도 클래스로 갈라 두었습니다.
 *
 * user 와 다른 점 셋
 *
 *   순서 역전 방어를 두지 않습니다.
 *   user 는 탈퇴가 가입보다 먼저 오면 삭제 표시 행을 만들어 두어야 했습니다.
 *   나중에 도착한 account.created 를 멈추기 위해서입니다.
 *   이 서비스는 account.created 를 구독하지 않습니다. 반려동물은 사용자가 직접 등록하고,
 *   탈퇴 뒤에 그 계정으로 등록하려면 로그인이 돼야 하는데 auth 가 계정을 이미 끊었습니다.
 *   표시 행을 두면 이름도 견종도 없는 반려동물 행이 생겨 말이 되지 않습니다.
 *
 *   상태를 갈래로 나누지 않습니다.
 *   user 는 소프트 딜리트라 "다시 익명화하면 삭제 시각이 흔들린다" 는 문제가 있었습니다.
 *   여기는 하드 딜리트라 지울 것이 없으면 0건으로 조용히 끝납니다.
 *
 *   사진을 접두사로 지우지 않습니다.
 *   pets/{accountId}/ 아래를 통째로 지우려면 ListObjectsV2 가 필요하고
 *   IAM 에 s3:ListBucket 을 열어야 합니다. 그 권한이 있으면 버킷 안을 훑을 수 있게 됩니다.
 *   한 사람이 가진 반려동물이 많아야 서너 마리라 하나씩 지우는 편이 낫습니다.
 *   등록하지 않고 이탈해 생긴 고아 파일은 이 경로로 지워지지 않으며,
 *   그것은 접두사 설계 덕에 나중에 수명 주기 규칙으로 치울 수 있습니다.
 *
 * @Transactional 을 붙이지 않습니다.
 * 이벤트 경로는 InboxProcessor.processOnce 가 이미 트랜잭션을 열고 있어
 * 여기에 또 붙이면 경계가 어디인지 읽는 사람이 매번 따져야 합니다.
 *
 * 트랜잭션 없이 불리면 아래 벌크 삭제가 그 자리에서 실패합니다.
 * 조용히 지나가지 않으므로 경계가 사라진 것을 모르고 넘어갈 일은 없습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountWithdrawnService {

    private final PetRepository petRepository;
    private final StorageProvider storageProvider;
    private final AfterCommitExecutor afterCommitExecutor;

    /**
     * 그 계정의 반려동물과 사진을 지웁니다.
     *
     * 순서가 셋입니다.
     *   ① 반려동물을 읽어 사진 키를 모읍니다
     *   ② 반려동물을 지웁니다
     *   ③ 커밋 뒤에 사진 객체를 지웁니다
     *
     * ①을 먼저 하는 이유는 지우고 나면 키를 찾을 방법이 없기 때문입니다.
     * 접두사로 나열하지 않기로 했으므로 행이 유일한 출처입니다.
     *
     * ②와 ③의 순서가 중요합니다.
     * 트랜잭션 안에서 객체를 지우면 롤백이 나도 객체는 안 돌아와
     * 반려동물은 남았는데 사진만 사라집니다.
     *
     * pet.profile.updated 를 발행하지 않습니다.
     * auth 가 이미 account.withdrawn 을 냈고 verdict 도 그것을 받을 수 있는 자리입니다.
     * 탈퇴 하나에 이벤트가 둘 나가면 받는 쪽이 어느 것을 봐야 하는지 흐려집니다.
     */
    public void withdraw(UUID accountId) {
        List<String> photoKeys = petRepository.findAllByAccountId(accountId).stream()
                .map(Pet::getPhotoUrl)
                .filter(Objects::nonNull)
                .toList();

        int deleted = petRepository.deleteAllByAccountId(accountId);

        log.info("반려동물을 정리했습니다: accountId={}, pet={}, photo={}",
                accountId, deleted, photoKeys.size());

        schedulePhotoCleanup(accountId, photoKeys);
    }

    /**
     * 커밋 이후에 사진을 지우도록 걸어 둡니다.
     *
     * 키마다 따로 걸어 둡니다.
     * 한 작업에 여러 개를 담으면 앞엣것이 실패했을 때 뒤엣것이 아예 실행되지 않고,
     * 무엇이 남았는지도 로그 한 줄로 뭉쳐 가릴 수 없습니다.
     *
     * 여기서 실패해도 요청은 성공으로 끝납니다. 커밋이 이미 끝난 뒤이기 때문입니다.
     * 남은 객체는 닿을 방법이 없습니다.
     * 버킷이 퍼블릭 액세스를 차단해 두었고 그 키를 아는 행이 사라졌기 때문입니다.
     */
    private void schedulePhotoCleanup(UUID accountId, List<String> photoKeys) {
        for (String key : photoKeys) {
            afterCommitExecutor.run(() -> storageProvider.delete(key),
                    "탈퇴 계정 사진 삭제 accountId=" + accountId + ", key=" + key);
        }
    }
}
