package com.pawtrail.pet.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pawtrail.pet.domain.enums.Species;
import com.pawtrail.pet.domain.model.Breed;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * V21 이 넣은 견종 마스터를 진짜 데이터베이스에서 확인합니다.
 *
 * 시드 45행은 손으로 쓴 값이고 정렬은 CASE 식이라 둘 다 확인하기 전에는
 * 틀려 있을 수 있는 부류입니다. 게이트웨이로 한 번 불러 보는 것은 그 순간만 보증하고
 * 남지 않으므로, 앞으로 견종을 더할 때 회귀를 잡아 주는 자리를 여기 둡니다.
 *
 * 이름 가나다순도 단언합니다.
 * 정렬을 데이터베이스가 아니라 저장소 구현이 하므로 이 검사는 콜레이션이 아니라
 * 우리 코드를 봅니다. 처음에는 질의에 ORDER BY 를 두고 이 단언을 뺐는데,
 * 그 사이에 실물에서 글자 수가 앞서는 순서가 나와도 빌드가 잡지 못했습니다.
 *
 * 컨테이너를 띄우는 이유는 PetApplicationTests 와 같습니다.
 * DataSource 주소가 설정 서버에서 내려오는데 spring.config.import 가 optional 이라
 * 설정 서버가 떠 있는지에 따라 결과가 갈리면 검사로서 의미가 없습니다.
 */
@SpringBootTest
@Testcontainers
class BreedSeedTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private BreedRepository breedRepository;

    @Test
    @DisplayName("견종 45행이 들어와 있다")
    void 견종_45행이_들어와_있다() {
        assertThat(breedRepository.findAllForDropdown()).hasSize(45);
    }

    @Test
    @DisplayName("MIX 와 OTHER 가 언제나 맨 끝이다")
    void MIX_와_OTHER_가_언제나_맨_끝이다() {
        List<Breed> breeds = breedRepository.findAllForDropdown();

        assertThat(breeds).extracting(Breed::getCode)
                .endsWith("MIX", "OTHER");
    }

    @Test
    @DisplayName("MIX 와 OTHER 를 뺀 나머지는 이름 가나다순이다")
    void MIX_와_OTHER_를_뺀_나머지는_이름_가나다순이다() {
        List<Breed> breeds = breedRepository.findAllForDropdown();
        List<String> names = breeds.subList(0, breeds.size() - 2).stream()
                .map(Breed::getNameKo)
                .toList();

        assertThat(names).isSorted();
    }

    @Test
    @DisplayName("말티즈가 글자 수가 아니라 이름 자리에 있다")
    void 말티즈가_글자_수가_아니라_이름_자리에_있다() {
        List<String> names = breedRepository.findAllForDropdown().stream()
                .map(Breed::getNameKo)
                .toList();

        // 콜레이션에 맡겼을 때 말티즈가 세 글자 무리로 밀려났던 자리임
        // 도베르만(ㄷ)보다 뒤, 불도그(ㅂ)보다 앞이면 이름 자리에 선 것임
        assertThat(names.indexOf("말티즈")).isGreaterThan(names.indexOf("도베르만"));
        assertThat(names.indexOf("말티즈")).isLessThan(names.indexOf("불도그"));
    }

    @Test
    @DisplayName("맹견은 시행규칙 제2조의 5종뿐이다")
    void 맹견은_시행규칙_제2조의_5종뿐이다() {
        List<String> dangerous = breedRepository.findAllForDropdown().stream()
                .filter(Breed::isDangerous)
                .map(Breed::getCode)
                .toList();

        assertThat(dangerous).containsExactlyInAnyOrder(
                "TOSA",
                "AMERICAN_PIT_BULL_TERRIER",
                "AMERICAN_STAFFORDSHIRE",
                "STAFFORDSHIRE_BULL_TERRIER",
                "ROTTWEILER");
    }

    @Test
    @DisplayName("OTHER 만 종이 ETC 이고 MIX 는 개다")
    void OTHER_만_종이_ETC_이고_MIX_는_개다() {
        List<String> notDog = breedRepository.findAllForDropdown().stream()
                .filter(breed -> breed.getSpecies() != Species.DOG)
                .map(Breed::getCode)
                .toList();

        assertThat(notDog).containsExactly("OTHER");
        assertThat(breedRepository.findByCode("MIX")).isPresent()
                .get()
                .extracting(Breed::getSpecies)
                .isEqualTo(Species.DOG);
    }
}
