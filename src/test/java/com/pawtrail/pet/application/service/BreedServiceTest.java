package com.pawtrail.pet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pawtrail.pet.application.dto.output.BreedOutput;
import com.pawtrail.pet.domain.model.Breed;
import com.pawtrail.pet.domain.repository.BreedRepository;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 견종 목록을 내어 주는 규칙을 검사합니다.
 *
 * 순서는 저장소가 정하므로 여기서는 그것을 다시 정렬하지 않는지만 봅니다.
 * 실제 정렬이 맞는지는 BreedSeedTest 가 진짜 DB 에서 확인합니다.
 */
@ExtendWith(MockitoExtension.class)
class BreedServiceTest {

    @Mock
    private BreedRepository breedRepository;

    @InjectMocks
    private BreedService breedService;

    @Test
    @DisplayName("코드와 이름만 담아 내보낸다")
    void 코드와_이름만_담아_내보낸다() {
        when(breedRepository.findAllForDropdown())
                .thenReturn(List.of(breed("MALTESE", "말티즈")));

        List<BreedOutput> result = breedService.getBreeds();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().code()).isEqualTo("MALTESE");
        assertThat(result.getFirst().nameKo()).isEqualTo("말티즈");
    }

    @Test
    @DisplayName("저장소가 준 순서를 그대로 유지한다")
    void 저장소가_준_순서를_그대로_유지한다() {
        when(breedRepository.findAllForDropdown()).thenReturn(List.of(
                breed("POODLE", "푸들"),
                breed("MALTESE", "말티즈"),
                breed("MIX", "믹스 · 목록에 없는 견종"),
                breed("OTHER", "그 외 (고양이 등)")));

        List<BreedOutput> result = breedService.getBreeds();

        assertThat(result).extracting(BreedOutput::code)
                .containsExactly("POODLE", "MALTESE", "MIX", "OTHER");
    }

    @Test
    @DisplayName("견종이 하나도 없으면 빈 목록을 준다")
    void 견종이_하나도_없으면_빈_목록을_준다() {
        when(breedRepository.findAllForDropdown()).thenReturn(List.of());

        assertThat(breedService.getBreeds()).isEmpty();
    }

    // Breed 는 값을 마이그레이션이 넣는 표라 생성 팩터리가 없음
    // 테스트에서만 필요한 값이라 리플렉션으로 채움
    private Breed breed(String code, String nameKo) {
        try {
            Breed instance = newInstance();
            set(instance, "code", code);
            set(instance, "nameKo", nameKo);
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("테스트용 Breed 를 만들지 못했습니다.", e);
        }
    }

    private Breed newInstance() throws ReflectiveOperationException {
        var constructor = Breed.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private void set(Breed instance, String name, Object value) throws ReflectiveOperationException {
        Field field = Breed.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }
}
