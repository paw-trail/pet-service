package com.pawtrail.pet.application.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 트랜잭션이 커밋된 뒤에 실행합니다.
 *
 * 왜 필요한가
 *
 * 객체 저장소는 트랜잭션에 묶이지 않습니다.
 * 데이터베이스 작업 사이에서 사진을 지우면, 뒤에서 롤백이 났을 때
 * 데이터베이스는 되돌아가는데 지운 쪽은 그대로라 둘이 어긋납니다.
 *   사진 교체   행은 옛 키를 가리키는데 그 객체가 이미 없어져 사진이 열리지 않음
 *   삭제        반려동물은 남았는데 사진만 사라짐
 *
 * 되돌릴 수 없는 쪽을 나중에 두는 것이기도 합니다.
 *
 * 공통 모듈의 OutboxCommitListener 도 같은 이유로 커밋 이후에 발행합니다.
 * 이 서비스만 다른 방식을 쓰면 같은 문제를 두 가지로 푸는 셈이 되므로 맞췄습니다.
 *
 * 감수하는 것
 *
 * 커밋이 끝난 뒤라 여기서 실패해도 호출자에게 전달되지 않습니다.
 * 로그만 남고 요청은 성공으로 끝나며, 그 객체는 지워지지 않은 채 남습니다.
 * 다만 남더라도 닿을 방법이 없어 큰 문제가 되지 않습니다.
 * 버킷이 퍼블릭 액세스를 차단해 두어 서명 없이는 열리지 않고,
 * 서명을 만들어 주는 조회가 그 키를 더 이상 돌려주지 않기 때문입니다.
 * 버킷의 수명 주기 규칙으로 나중에 치울 수 있습니다.
 *
 * 왜 한 번에 하나씩 넘기는가
 *
 * 아래 run 이 실패를 잡아 삼키는 단위가 넘겨받은 작업 하나입니다.
 * 여러 가지를 한 작업에 담으면 앞엣것이 실패했을 때 뒤엣것이 아예 실행되지 않고,
 * 무엇이 남았는지도 설명 한 줄로 뭉쳐 로그만 보고는 가릴 수 없습니다.
 *
 * auth 와 user 에도 같은 클래스가 있습니다
 *
 * 공통 모듈에 올리지 않은 것은 쓰는 서비스가 적기 때문입니다.
 * 세 번째가 되었으나 서비스 17개 중 셋이라 비율은 크게 달라지지 않았고,
 * 공통 모듈은 버전을 올리면 전 서비스가 그 버전을 물어야 합니다.
 * review 까지 보고 한 번에 판단합니다.
 */
@Slf4j
@Component
public class AfterCommitExecutor {

    /**
     * @param action      커밋 이후에 실행할 일입니다.
     * @param description 실패했을 때 로그에 남길 이름입니다.
     *                    무엇이 실패했는지가 로그만으로 드러나야 하므로 받습니다.
     */
    public void run(Runnable action, String description) {

        // 트랜잭션이 없으면 그냥 바로 실행함
        //
        // 테스트에서 트랜잭션 없이 부르는 경우가 있는데,
        // 그때 조용히 건너뛰면 "실행됐다고 생각했는데 안 된" 상태가 됩니다.
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (Exception e) {
                    // 여기서 던지면 이미 끝난 트랜잭션 밖으로 나가 아무도 받지 않음
                    // 남길 수 있는 것이 로그뿐이므로 무엇이 실패했는지를 적어 둡니다.
                    log.error("커밋 이후 작업에 실패했습니다: {}", description, e);
                }
            }
        });
    }
}
