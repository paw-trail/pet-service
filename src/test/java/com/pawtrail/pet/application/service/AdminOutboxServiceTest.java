package com.pawtrail.pet.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.pawtrail.common.exception.CustomException;
import com.pawtrail.common.message.outbox.OutboxPublisher;
import com.pawtrail.common.message.outbox.OutboxRepository;
import com.pawtrail.pet.domain.exception.PetErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 관리자 재발행의 규칙을 검사합니다.
 *
 * 핵심은 실패를 성공으로 응답하지 않는 것입니다.
 * 보냈다고 알고 넘어가는 상태가 바로 이 기능이 막으려던 것입니다.
 *
 * 조회는 공통 모듈의 질의와 PageResponse 변환뿐이라 목으로 확인할 것이 없습니다.
 */
@ExtendWith(MockitoExtension.class)
class AdminOutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxPublisher outboxPublisher;

    @InjectMocks
    private AdminOutboxService adminOutboxService;

    @Test
    @DisplayName("발행에 성공하면 조용히 끝난다")
    void 발행에_성공하면_조용히_끝난다() {
        UUID outboxId = UUID.randomUUID();
        when(outboxPublisher.publish(outboxId)).thenReturn(true);

        assertThatCode(() -> adminOutboxService.republish(outboxId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("발행에 실패하면 성공으로 응답하지 않는다")
    void 발행에_실패하면_성공으로_응답하지_않는다() {
        UUID outboxId = UUID.randomUUID();
        when(outboxPublisher.publish(outboxId)).thenReturn(false);

        assertThatThrownBy(() -> adminOutboxService.republish(outboxId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.OUTBOX_REPUBLISH_FAILED);
    }
}
