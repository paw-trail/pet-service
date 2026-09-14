package com.pawtrail.pet.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawtrail.pet.domain.enums.BreedSize;
import com.pawtrail.pet.domain.model.Pet;
import com.pawtrail.pet.domain.provider.StorageProvider;
import com.pawtrail.pet.domain.repository.PetRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 탈퇴한 계정을 정리하는 규칙을 검사합니다.
 *
 * 지우기 전에 사진 키를 모으는지, 사진이 없는 반려동물을 건너뛰는지,
 * 지울 것이 없어도 조용히 끝나는지를 봅니다.
 *
 * 사진 삭제가 실패하면 예외가 밖으로 나가야 합니다.
 * 이 경로는 커밋 이후로 미루지 않습니다.
 * 미루면 실패가 로그 한 줄로 끝나고 소비는 성공으로 처리되어 재시도도 DLQ 도 돌지 않는데,
 * 행을 이미 지워 그 키를 다시 찾을 길이 없어 사진이 영영 남습니다.
 */
@ExtendWith(MockitoExtension.class)
class AccountWithdrawnServiceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Mock
    private PetRepository petRepository;

    @Mock
    private StorageProvider storageProvider;

    @InjectMocks
    private AccountWithdrawnService accountWithdrawnService;

    @Test
    @DisplayName("반려동물과 사진을 모두 지운다")
    void 반려동물과_사진을_모두_지운다() {
        when(petRepository.findAllByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(pet("pets/acc/a"), pet("pets/acc/b")));
        when(petRepository.deleteAllByAccountId(ACCOUNT_ID)).thenReturn(2);

        accountWithdrawnService.withdraw(ACCOUNT_ID);

        verify(petRepository).deleteAllByAccountId(ACCOUNT_ID);
        verify(storageProvider).delete("pets/acc/a");
        verify(storageProvider).delete("pets/acc/b");
    }

    @Test
    @DisplayName("사진이 없는 반려동물은 건너뛴다")
    void 사진이_없는_반려동물은_건너뛴다() {
        when(petRepository.findAllByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(pet(null), pet("pets/acc/only")));
        when(petRepository.deleteAllByAccountId(ACCOUNT_ID)).thenReturn(2);

        accountWithdrawnService.withdraw(ACCOUNT_ID);

        verify(storageProvider).delete("pets/acc/only");
        verify(storageProvider, never()).delete(null);
    }

    @Test
    @DisplayName("지울 것이 없어도 조용히 끝난다")
    void 지울_것이_없어도_조용히_끝난다() {
        when(petRepository.findAllByAccountId(ACCOUNT_ID)).thenReturn(List.of());
        when(petRepository.deleteAllByAccountId(ACCOUNT_ID)).thenReturn(0);

        accountWithdrawnService.withdraw(ACCOUNT_ID);

        verify(storageProvider, never()).delete(anyString());
    }

    @Test
    @DisplayName("지우기 전에 사진 키를 먼저 모은다")
    void 지우기_전에_사진_키를_먼저_모은다() {
        when(petRepository.findAllByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(pet("pets/acc/a")));
        when(petRepository.deleteAllByAccountId(ACCOUNT_ID)).thenReturn(1);

        accountWithdrawnService.withdraw(ACCOUNT_ID);

        // 지운 뒤에는 키를 찾을 방법이 없음 — 접두사로 나열하지 않기로 했기 때문임
        InOrder order = Mockito.inOrder(petRepository);
        order.verify(petRepository).findAllByAccountId(eq(ACCOUNT_ID));
        order.verify(petRepository).deleteAllByAccountId(eq(ACCOUNT_ID));
    }

    @Test
    @DisplayName("사진 삭제가 실패하면 예외를 그대로 내보낸다")
    void 사진_삭제가_실패하면_예외를_그대로_내보낸다() {
        when(petRepository.findAllByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(pet("pets/acc/a")));
        when(petRepository.deleteAllByAccountId(ACCOUNT_ID)).thenReturn(1);
        doThrow(new IllegalStateException("S3 실패")).when(storageProvider).delete("pets/acc/a");

        // 여기서 잡아 삼키면 소비가 성공으로 끝나 재시도도 DLQ 도 돌지 않음
        assertThatThrownBy(() -> accountWithdrawnService.withdraw(ACCOUNT_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    private Pet pet(String photoKey) {
        return Pet.create(ACCOUNT_ID, "몽이", "MALTESE", BreedSize.SMALL,
                new BigDecimal("5.0"), true, false, true, true, photoKey, null);
    }
}
