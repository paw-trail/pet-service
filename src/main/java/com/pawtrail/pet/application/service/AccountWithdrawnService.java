package com.pawtrail.pet.application.service;

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
 * AfterCommitExecutor 를 쓰지 않습니다
 *
 * 수정과 삭제 API 는 커밋 이후로 미뤄 둡니다.
 * 트랜잭션 안에서 객체를 지우면 롤백이 났을 때 반려동물은 남았는데 사진만 사라져
 * 프로필에 열리지 않는 사진이 박히기 때문입니다.
 *
 * 이 경로는 그 위험이 없습니다. 전부 지우는 일이라 롤백되면 아무것도 안 지워진 상태로 돌아갑니다.
 * 오히려 미뤄 두면 삭제 실패가 로그 한 줄로 끝납니다.
 * 소비는 성공으로 끝나 재시도도 DLQ 도 돌지 않고, 행을 이미 지워 그 키를 다시 찾을 길도 없습니다.
 * 그러면 탈퇴 시 사용자 데이터를 지운다는 약속을 못 지키게 됩니다.
 *
 * 트랜잭션 안에서 지우면 실패가 소비 실패로 이어집니다.
 * 공통 모듈의 DefaultErrorHandler 가 세 번 다시 시도하고 그래도 안 되면 DLQ 로 보내며,
 * 관리자가 재발행하면 처음부터 다시 합니다.
 *
 * 일부만 지운 뒤 실패해도 괜찮습니다.
 * 재발행하면 없는 키를 지우는 것이 되는데 S3 는 그때 오류를 내지 않습니다.
 *
 * 전용 삭제 큐를 만들지 않습니다.
 * 표와 배치와 재시도 규칙이 새로 붙어 outbox 와 inbox 와 별개인 세 번째 장치가 됩니다.
 * DLQ 조회 API 조차 "그 도구의 재구현" 이라며 만들지 않은 기준과 어긋납니다.
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
     * ③을 트랜잭션 안에서 합니다.
     * 여기서 실패하면 소비가 실패로 끝나 재시도와 DLQ 가 걸립니다.
     * 커밋 이후로 미루면 실패가 로그 한 줄로 끝나는데,
     * 행을 이미 지워 그 키를 다시 찾을 길이 없어 사진이 영영 남습니다.
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

        deletePhotos(photoKeys);

        log.info("반려동물을 정리했습니다: accountId={}, pet={}, photo={}",
                accountId, deleted, photoKeys.size());
    }

    /**
     * 사진을 지웁니다.
     *
     * 예외를 잡지 않습니다.
     * 하나라도 실패하면 트랜잭션이 되돌아가고 소비도 실패로 끝나야
     * 재시도와 DLQ 가 걸려 다시 시도할 기회가 남습니다.
     *
     * 일부만 지운 뒤 실패해도 괜찮습니다.
     * 재발행하면 없는 키를 지우는 것이 되는데 S3 는 그때 오류를 내지 않습니다.
     */
    private void deletePhotos(List<String> photoKeys) {
        for (String key : photoKeys) {
            storageProvider.delete(key);
        }
    }
}
