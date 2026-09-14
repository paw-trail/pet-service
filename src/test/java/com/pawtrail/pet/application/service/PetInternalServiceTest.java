package com.pawtrail.pet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawtrail.common.exception.CustomException;
import com.pawtrail.common.message.outbox.OutboxEventRecorder;
import com.pawtrail.pet.application.dto.output.PetInternalOutput;
import com.pawtrail.pet.application.support.AfterCommitExecutor;
import com.pawtrail.pet.domain.enums.BreedSize;
import com.pawtrail.pet.domain.enums.Species;
import com.pawtrail.pet.domain.exception.PetErrorCode;
import com.pawtrail.pet.domain.model.Breed;
import com.pawtrail.pet.domain.model.Pet;
import com.pawtrail.pet.domain.provider.StorageProvider;
import com.pawtrail.pet.domain.repository.BreedRepository;
import com.pawtrail.pet.domain.repository.PetRepository;
import com.pawtrail.pet.infrastructure.config.StorageProperties;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 다른 서비스에 내어 주는 조회의 규칙을 검사합니다.
 *
 * 핵심은 소유권입니다.
 * 이 경로에는 토큰이 실려 오지 않지만 그것이 검증 면제를 뜻하지 않습니다.
 * 남의 반려동물은 결과에서 빠지고, 단건은 없는 것과 같은 응답을 냅니다.
 *
 * 헤더가 없을 때 거절하는 것은 컨트롤러가 합니다.
 * 인증 주체가 없으면 @CurrentUser 가 null 로 들어오기 때문입니다.
 */
@ExtendWith(MockitoExtension.class)
class PetInternalServiceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID OTHER_ACCOUNT_ID = UUID.randomUUID();

    @Mock
    private PetRepository petRepository;

    @Mock
    private BreedRepository breedRepository;

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private StorageProperties storageProperties;

    @Mock
    private OutboxEventRecorder outboxEventRecorder;

    @Mock
    private AfterCommitExecutor afterCommitExecutor;

    @InjectMocks
    private PetService petService;

    @Test
    @DisplayName("남의 반려동물은 결과에서 뺀다")
    void 남의_반려동물은_결과에서_뺀다() {
        Pet mine = pet(ACCOUNT_ID);
        Pet other = pet(OTHER_ACCOUNT_ID);
        when(petRepository.findAllByIdIn(anyCollection())).thenReturn(List.of(mine, other));
        when(breedRepository.findAllByCodeIn(anyCollection())).thenReturn(List.of(maltese()));

        List<PetInternalOutput> result =
                petService.getInternalPets(ACCOUNT_ID, List.of(mine.getId(), other.getId()));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().petId()).isEqualTo(mine.getId());
    }

    @Test
    @DisplayName("내 것이 하나도 없으면 견종을 아예 안 읽는다")
    void 내_것이_하나도_없으면_견종을_아예_안_읽는다() {
        Pet other = pet(OTHER_ACCOUNT_ID);
        when(petRepository.findAllByIdIn(anyCollection())).thenReturn(List.of(other));

        assertThat(petService.getInternalPets(ACCOUNT_ID, List.of(other.getId()))).isEmpty();

        verify(breedRepository, never()).findAllByCodeIn(anyCollection());
    }

    @Test
    @DisplayName("빈 목록으로 부르면 조회하지 않는다")
    void 빈_목록으로_부르면_조회하지_않는다() {
        assertThat(petService.getInternalPets(ACCOUNT_ID, List.of())).isEmpty();

        verify(petRepository, never()).findAllByIdIn(anyCollection());
    }

    @Test
    @DisplayName("사진과 메모를 담지 않는다")
    void 사진과_메모를_담지_않는다() {
        Pet mine = pet(ACCOUNT_ID);
        when(petRepository.findAllByIdIn(anyCollection())).thenReturn(List.of(mine));
        when(breedRepository.findAllByCodeIn(anyCollection())).thenReturn(List.of(maltese()));

        petService.getInternalPets(ACCOUNT_ID, List.of(mine.getId()));

        // 사진을 안 담으므로 서명을 만들 일이 없음
        verify(storageProvider, never()).presignDownload(anyString());
    }

    @Test
    @DisplayName("단건은 남의 것이면 없는 것과 같다")
    void 단건은_남의_것이면_없는_것과_같다() {
        Pet other = pet(OTHER_ACCOUNT_ID);
        when(petRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> petService.getInternalPet(ACCOUNT_ID, other.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.PET_NOT_FOUND);
    }

    @Test
    @DisplayName("단건은 견종이 없어도 나머지를 채워 준다")
    void 단건은_견종이_없어도_나머지를_채워_준다() {
        Pet mine = pet(ACCOUNT_ID);
        when(petRepository.findById(mine.getId())).thenReturn(Optional.of(mine));
        // breed_code 에 외래 키가 없어 실제로 생길 수 있는 상태임
        when(breedRepository.findByCode("MALTESE")).thenReturn(Optional.empty());

        PetInternalOutput result = petService.getInternalPet(ACCOUNT_ID, mine.getId());

        assertThat(result.breedName()).isNull();
        assertThat(result.species()).isNull();
        assertThat(result.isDangerousBreed()).isFalse();
        assertThat(result.weightKg()).isEqualByComparingTo("5.0");
    }

    private Pet pet(UUID accountId) {
        Pet instance = Pet.create(accountId, "몽이", "MALTESE", BreedSize.SMALL,
                new BigDecimal("5.0"), true, false, true, true, "pets/acc/photo", "메모");
        setField(instance, "id", UUID.randomUUID());
        return instance;
    }

    private Breed maltese() {
        try {
            var constructor = Breed.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Breed instance = constructor.newInstance();
            setField(instance, "code", "MALTESE");
            setField(instance, "nameKo", "말티즈");
            setField(instance, "species", Species.DOG);
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("테스트용 Breed 를 만들지 못했습니다.", e);
        }
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("테스트 값을 넣지 못했습니다: " + name, e);
        }
    }
}
