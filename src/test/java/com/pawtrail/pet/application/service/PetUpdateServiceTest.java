package com.pawtrail.pet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pawtrail.common.exception.CustomException;
import com.pawtrail.common.message.DomainEvent;
import com.pawtrail.common.message.outbox.OutboxEventRecorder;
import com.pawtrail.pet.application.dto.input.PetUpdateInput;
import com.pawtrail.pet.application.dto.output.PetOutput;
import com.pawtrail.pet.application.support.AfterCommitExecutor;
import com.pawtrail.pet.domain.enums.BreedSize;
import com.pawtrail.pet.domain.enums.Species;
import com.pawtrail.pet.domain.event.payload.PetProfileUpdatedEvent;
import com.pawtrail.pet.domain.exception.PetErrorCode;
import com.pawtrail.pet.domain.model.Breed;
import com.pawtrail.pet.domain.model.Pet;
import com.pawtrail.pet.domain.provider.StorageProvider;
import com.pawtrail.pet.domain.repository.BreedRepository;
import com.pawtrail.pet.domain.repository.PetRepository;
import com.pawtrail.pet.infrastructure.config.StorageProperties;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 반려동물을 고치고 지우는 규칙을 검사합니다.
 *
 * 부분 수정이라 "안 보낸 것은 그대로" 가 지켜지는지가 핵심입니다.
 * 이벤트는 값이 실제로 달라졌을 때만 나가고,
 * 판정 축이 아닌 것만 바뀌면 verdictRelevantChanged 가 거짓입니다.
 */
@ExtendWith(MockitoExtension.class)
class PetUpdateServiceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

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
    @DisplayName("안 보낸 필드는 그대로 둔다")
    void 안_보낸_필드는_그대로_둔다() {
        Pet pet = pet();
        given(pet);

        petService.update(ACCOUNT_ID, pet.getId(), empty());

        assertThat(pet.getName()).isEqualTo("몽이");
        assertThat(pet.getWeightKg()).isEqualByComparingTo("5.0");
        assertThat(pet.getNote()).isEqualTo("원래 메모");
    }

    @Test
    @DisplayName("이름만 바꾸면 판정과 무관한 변경이다")
    void 이름만_바꾸면_판정과_무관한_변경이다() {
        Pet pet = pet();
        given(pet);

        petService.update(ACCOUNT_ID, pet.getId(), nameOnly("새이름"));

        assertThat(pet.getName()).isEqualTo("새이름");
        assertThat(captureEvent().verdictRelevantChanged()).isFalse();
    }

    @Test
    @DisplayName("체중만 바꾸면 크기를 다시 계산한다")
    void 체중만_바꾸면_크기를_다시_계산한다() {
        Pet pet = pet();
        given(pet);

        petService.update(ACCOUNT_ID, pet.getId(), weightOnly(new BigDecimal("30.0")));

        assertThat(pet.getBreedSize()).isEqualTo(BreedSize.LARGE);
        assertThat(captureEvent().verdictRelevantChanged()).isTrue();
    }

    @Test
    @DisplayName("크기를 함께 보내면 그 값이 계산을 이긴다")
    void 크기를_함께_보내면_그_값이_계산을_이긴다() {
        Pet pet = pet();
        given(pet);

        PetUpdateInput input = new PetUpdateInput(
                false, null, false, null,
                true, new BigDecimal("30.0"),
                true, BreedSize.SMALL,
                false, null, false, null, false, null, false, null,
                false, null, false, null);

        petService.update(ACCOUNT_ID, pet.getId(), input);

        assertThat(pet.getBreedSize()).isEqualTo(BreedSize.SMALL);
    }

    @Test
    @DisplayName("같은 값을 다시 보내면 이벤트를 내보내지 않는다")
    void 같은_값을_다시_보내면_이벤트를_내보내지_않는다() {
        Pet pet = pet();
        given(pet);

        // 지금과 똑같은 체중을 자릿수만 다르게 보냄
        petService.update(ACCOUNT_ID, pet.getId(), weightOnly(new BigDecimal("5.00")));

        verify(outboxEventRecorder, never()).record(any(DomainEvent.class));
    }

    @Test
    @DisplayName("사진을 바꾸면 옛 객체를 지운다")
    void 사진을_바꾸면_옛_객체를_지운다() {
        Pet pet = pet();
        setField(pet, "photoUrl", "pets/acc/old");
        given(pet);
        when(storageProvider.extractOwnedKey(anyString(), any(UUID.class)))
                .thenReturn(Optional.of("pets/acc/new"));

        PetUpdateInput input = new PetUpdateInput(
                false, null, false, null, false, null, false, null,
                false, null, false, null, false, null, false, null,
                true, "https://bucket/pets/acc/new", false, null);

        petService.update(ACCOUNT_ID, pet.getId(), input);

        assertThat(pet.getPhotoUrl()).isEqualTo("pets/acc/new");
        verify(afterCommitExecutor).run(any(Runnable.class), anyString());
    }

    @Test
    @DisplayName("지우면 판정과 관련된 변경으로 알린다")
    void 지우면_판정과_관련된_변경으로_알린다() {
        Pet pet = pet();
        when(petRepository.findById(pet.getId())).thenReturn(Optional.of(pet));

        petService.delete(ACCOUNT_ID, pet.getId());

        verify(petRepository).delete(pet);
        assertThat(captureEvent().verdictRelevantChanged()).isTrue();
    }

    @Test
    @DisplayName("저장된 견종이 목록에서 사라져도 이름은 고칠 수 있다")
    void 저장된_견종이_목록에서_사라져도_이름은_고칠_수_있다() {
        Pet pet = pet();
        when(petRepository.findById(pet.getId())).thenReturn(Optional.of(pet));
        // breed_code 에 외래 키가 없어 실제로 생길 수 있는 상태임
        when(breedRepository.findByCode("MALTESE")).thenReturn(Optional.empty());

        PetOutput result = petService.update(ACCOUNT_ID, pet.getId(), nameOnly("새이름"));

        assertThat(result.breedName()).isNull();
        assertThat(result.species()).isNull();
        assertThat(pet.getName()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("견종을 바꿀 때만 없는 코드를 막는다")
    void 견종을_바꿀_때만_없는_코드를_막는다() {
        Pet pet = pet();
        when(petRepository.findById(pet.getId())).thenReturn(Optional.of(pet));
        when(breedRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        PetUpdateInput input = new PetUpdateInput(
                false, null, true, "NOPE", false, null, false, null,
                false, null, false, null, false, null, false, null,
                false, null, false, null);

        assertThatThrownBy(() -> petService.update(ACCOUNT_ID, pet.getId(), input))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("사진은 명시적 null 로만 지워진다")
    void 사진은_명시적_null_로만_지워진다() {
        Pet pet = pet();
        setField(pet, "photoUrl", "pets/acc/old");
        given(pet);

        PetUpdateInput input = new PetUpdateInput(
                false, null, false, null, false, null, false, null,
                false, null, false, null, false, null, false, null,
                true, null, false, null);

        petService.update(ACCOUNT_ID, pet.getId(), input);

        assertThat(pet.getPhotoUrl()).isNull();
        verify(afterCommitExecutor).run(any(Runnable.class), anyString());
    }

    @Test
    @DisplayName("남의 반려동물은 고칠 수 없다")
    void 남의_반려동물은_고칠_수_없다() {
        Pet other = pet();
        setField(other, "accountId", UUID.randomUUID());
        when(petRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> petService.update(ACCOUNT_ID, other.getId(), nameOnly("x")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.PET_NOT_FOUND);
    }

    private void given(Pet pet) {
        when(petRepository.findById(pet.getId())).thenReturn(Optional.of(pet));
        when(breedRepository.findByCode("MALTESE")).thenReturn(Optional.of(maltese()));
    }

    private PetProfileUpdatedEvent captureEvent() {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(outboxEventRecorder).record(captor.capture());
        return (PetProfileUpdatedEvent) captor.getValue();
    }

    // 아무 필드도 안 보낸 요청임
    private PetUpdateInput empty() {
        return new PetUpdateInput(
                false, null, false, null, false, null, false, null,
                false, null, false, null, false, null, false, null,
                false, null, false, null);
    }

    private PetUpdateInput nameOnly(String name) {
        return new PetUpdateInput(
                true, name, false, null, false, null, false, null,
                false, null, false, null, false, null, false, null,
                false, null, false, null);
    }

    private PetUpdateInput weightOnly(BigDecimal weightKg) {
        return new PetUpdateInput(
                false, null, false, null, true, weightKg, false, null,
                false, null, false, null, false, null, false, null,
                false, null, false, null);
    }

    private Pet pet() {
        Pet instance = Pet.create(ACCOUNT_ID, "몽이", "MALTESE", BreedSize.SMALL,
                new BigDecimal("5.0"), true, false, true, true, null, "원래 메모");
        setField(instance, "id", UUID.randomUUID());
        return instance;
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
}
