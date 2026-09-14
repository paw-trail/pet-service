package com.pawtrail.pet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawtrail.common.exception.CustomException;
import com.pawtrail.pet.application.dto.input.PetCreateInput;
import com.pawtrail.pet.application.dto.output.PetOutput;
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
 * 반려동물을 등록하고 조회하는 규칙을 검사합니다.
 *
 * 크기 계산 자체는 BreedSizeTest 가 경계값으로 봅니다.
 * 여기서는 "사용자가 보낸 값이 계산을 이기는가" 를 봅니다.
 */
@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Mock
    private PetRepository petRepository;

    @Mock
    private BreedRepository breedRepository;

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private StorageProperties storageProperties;

    @InjectMocks
    private PetService petService;

    @Test
    @DisplayName("크기를 안 보내면 체중에서 계산한다")
    void 크기를_안_보내면_체중에서_계산한다() {
        when(breedRepository.findByCode("MALTESE")).thenReturn(Optional.of(maltese()));
        when(petRepository.save(any(Pet.class))).thenAnswer(call -> call.getArgument(0));

        PetOutput result = petService.create(ACCOUNT_ID, input(new BigDecimal("12.0"), null));

        assertThat(result.breedSize()).isEqualTo(BreedSize.MEDIUM);
    }

    @Test
    @DisplayName("크기를 보내면 그 값이 계산을 이긴다")
    void 크기를_보내면_그_값이_계산을_이긴다() {
        when(breedRepository.findByCode("MALTESE")).thenReturn(Optional.of(maltese()));
        when(petRepository.save(any(Pet.class))).thenAnswer(call -> call.getArgument(0));

        // 체중으로 계산하면 MEDIUM 이지만 사용자가 SMALL 을 골랐음
        PetOutput result =
                petService.create(ACCOUNT_ID, input(new BigDecimal("12.0"), BreedSize.SMALL));

        assertThat(result.breedSize()).isEqualTo(BreedSize.SMALL);
    }

    @Test
    @DisplayName("없는 견종이면 등록하지 않는다")
    void 없는_견종이면_등록하지_않는다() {
        when(breedRepository.findByCode("MALTESE")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                petService.create(ACCOUNT_ID, input(new BigDecimal("5.0"), null)))
                .isInstanceOf(CustomException.class);

        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    @DisplayName("사진 주소가 내 자리가 아니면 등록하지 않는다")
    void 사진_주소가_내_자리가_아니면_등록하지_않는다() {
        when(breedRepository.findByCode("MALTESE")).thenReturn(Optional.of(maltese()));
        when(storageProvider.extractOwnedKey(anyString(), eq(ACCOUNT_ID)))
                .thenReturn(Optional.empty());

        PetCreateInput input = new PetCreateInput("몽이", "MALTESE", new BigDecimal("5.0"),
                null, true, false, true, true,
                "https://example.com/남의사진", null);

        assertThatThrownBy(() -> petService.create(ACCOUNT_ID, input))
                .isInstanceOf(CustomException.class);

        verify(petRepository, never()).save(any(Pet.class));
    }

    @Test
    @DisplayName("남의 반려동물은 못 본다")
    void 남의_반려동물은_못_본다() {
        Pet other = pet(UUID.randomUUID());
        when(petRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> petService.getPet(ACCOUNT_ID, other.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.PET_NOT_FOUND);
    }

    @Test
    @DisplayName("없는 반려동물도 같은 응답을 낸다")
    void 없는_반려동물도_같은_응답을_낸다() {
        UUID petId = UUID.randomUUID();
        when(petRepository.findById(petId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.getPet(ACCOUNT_ID, petId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.PET_NOT_FOUND);
    }

    @Test
    @DisplayName("목록은 견종을 한 번에 읽는다")
    void 목록은_견종을_한_번에_읽는다() {
        when(petRepository.findAllByAccountIdOrderByCreatedAtAsc(ACCOUNT_ID))
                .thenReturn(List.of(pet(ACCOUNT_ID), pet(ACCOUNT_ID)));
        when(breedRepository.findAllByCodeIn(anyCollection())).thenReturn(List.of(maltese()));

        List<PetOutput> result = petService.getMyPets(ACCOUNT_ID);

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().breedName()).isEqualTo("말티즈");
        // 두 마리인데 견종 조회는 한 번뿐이어야 함
        verify(breedRepository, never()).findByCode(anyString());
    }

    @Test
    @DisplayName("반려동물이 없으면 견종을 아예 안 읽는다")
    void 반려동물이_없으면_견종을_아예_안_읽는다() {
        when(petRepository.findAllByAccountIdOrderByCreatedAtAsc(ACCOUNT_ID))
                .thenReturn(List.of());

        assertThat(petService.getMyPets(ACCOUNT_ID)).isEmpty();

        verify(breedRepository, never()).findAllByCodeIn(anyCollection());
    }

    @Test
    @DisplayName("이미지가 상한을 넘으면 주소를 발급하지 않는다")
    void 이미지가_상한을_넘으면_주소를_발급하지_않는다() {
        when(storageProperties.maxImageBytes()).thenReturn(1000L);

        assertThatThrownBy(() ->
                petService.issueUploadUrl(ACCOUNT_ID, "image/png", 1001L))
                .isInstanceOf(CustomException.class);

        verify(storageProvider, never()).newPhotoKey(any(UUID.class));
    }

    private PetCreateInput input(BigDecimal weightKg, BreedSize breedSize) {
        return new PetCreateInput("몽이", "MALTESE", weightKg, breedSize,
                true, false, true, true, null, null);
    }

    // breed 는 값을 마이그레이션이 넣는 표라 생성 팩터리가 없음
    // 테스트에서만 필요한 값이라 리플렉션으로 채움
    private Breed maltese() {
        try {
            var constructor = Breed.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Breed instance = constructor.newInstance();
            set(instance, "code", "MALTESE");
            set(instance, "nameKo", "말티즈");
            set(instance, "dangerous", false);
            set(instance, "species", Species.DOG);
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("테스트용 Breed 를 만들지 못했습니다.", e);
        }
    }

    private void set(Breed instance, String name, Object value) throws ReflectiveOperationException {
        Field field = Breed.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }

    // 식별자는 애플리케이션이 저장 시점에 발급하므로 테스트에서 직접 넣어 줌
    private Pet pet(UUID accountId) {
        Pet instance = Pet.create(accountId, "몽이", "MALTESE", BreedSize.SMALL,
                new BigDecimal("5.0"), true, false, true, true, null, null);
        try {
            Field field = Pet.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(instance, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("테스트용 Pet 을 만들지 못했습니다.", e);
        }
        return instance;
    }
}
