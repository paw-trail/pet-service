package com.pawtrail.pet.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawtrail.pet.application.support.AfterCommitExecutor;
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
 * 커밋 이후 실행은 AfterCommitExecutor 가 맡으므로 여기서는 걸렸는지만 확인합니다.
 */
@ExtendWith(MockitoExtension.class)
class AccountWithdrawnServiceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Mock
    private PetRepository petRepository;

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private AfterCommitExecutor afterCommitExecutor;

    @InjectMocks
    private AccountWithdrawnService accountWithdrawnService;

    @Test
    @DisplayName("반려동물을 지우고 사진 삭제를 커밋 뒤로 걸어 둔다")
    void 반려동물을_지우고_사진_삭제를_커밋_뒤로_걸어_둔다() {
        when(petRepository.findAllByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(pet("pets/acc/a"), pet("pets/acc/b")));
        when(petRepository.deleteAllByAccountId(ACCOUNT_ID)).thenReturn(2);

        accountWithdrawnService.withdraw(ACCOUNT_ID);

        verify(petRepository).deleteAllByAccountId(ACCOUNT_ID);
        // 키마다 따로 걸어야 하나가 실패해도 나머지가 돎
        verify(afterCommitExecutor, times(2)).run(any(Runnable.class), anyString());
    }

    @Test
    @DisplayName("사진이 없는 반려동물은 삭제를 걸지 않는다")
    void 사진이_없는_반려동물은_삭제를_걸지_않는다() {
        when(petRepository.findAllByAccountId(ACCOUNT_ID))
                .thenReturn(List.of(pet(null), pet("pets/acc/only")));
        when(petRepository.deleteAllByAccountId(ACCOUNT_ID)).thenReturn(2);

        accountWithdrawnService.withdraw(ACCOUNT_ID);

        verify(afterCommitExecutor, times(1)).run(any(Runnable.class), anyString());
    }

    @Test
    @DisplayName("지울 것이 없어도 조용히 끝난다")
    void 지울_것이_없어도_조용히_끝난다() {
        when(petRepository.findAllByAccountId(ACCOUNT_ID)).thenReturn(List.of());
        when(petRepository.deleteAllByAccountId(ACCOUNT_ID)).thenReturn(0);

        accountWithdrawnService.withdraw(ACCOUNT_ID);

        verify(afterCommitExecutor, never()).run(any(Runnable.class), anyString());
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

    private Pet pet(String photoKey) {
        return Pet.create(ACCOUNT_ID, "몽이", "MALTESE", BreedSize.SMALL,
                new BigDecimal("5.0"), true, false, true, true, photoKey, null);
    }
}
