package com.pawtrail.pet.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 체중에서 크기를 정하는 규칙을 검사합니다.
 *
 * 순수 함수라 데이터베이스도 목도 필요하지 않습니다.
 * 경계값을 못 박아 두는 것이 이 검사의 목적입니다.
 * 국립축산과학원 기준이 부등호를 명시하고 있어 10.0 은 MEDIUM, 25.0 은 LARGE 입니다.
 */
class BreedSizeTest {

    @Test
    @DisplayName("10kg 미만은 소형이다")
    void _10kg_미만은_소형이다() {
        assertThat(size("0.1")).isEqualTo(BreedSize.SMALL);
        assertThat(size("5.4")).isEqualTo(BreedSize.SMALL);
        assertThat(size("9.9")).isEqualTo(BreedSize.SMALL);
    }

    @Test
    @DisplayName("10kg 이상 25kg 미만은 중형이다")
    void _10kg_이상_25kg_미만은_중형이다() {
        assertThat(size("10.0")).isEqualTo(BreedSize.MEDIUM);
        assertThat(size("10.1")).isEqualTo(BreedSize.MEDIUM);
        assertThat(size("24.9")).isEqualTo(BreedSize.MEDIUM);
    }

    @Test
    @DisplayName("25kg 이상은 대형이다")
    void _25kg_이상은_대형이다() {
        assertThat(size("25.0")).isEqualTo(BreedSize.LARGE);
        assertThat(size("25.1")).isEqualTo(BreedSize.LARGE);
        assertThat(size("200.0")).isEqualTo(BreedSize.LARGE);
    }

    @Test
    @DisplayName("자릿수가 달라도 같은 값으로 본다")
    void 자릿수가_달라도_같은_값으로_본다() {
        // compareTo 가 아니라 equals 로 견주면 10 과 10.0 이 다른 값이 됨
        assertThat(size("10")).isEqualTo(BreedSize.MEDIUM);
        assertThat(size("10.00")).isEqualTo(BreedSize.MEDIUM);
        assertThat(size("25")).isEqualTo(BreedSize.LARGE);
    }

    @Test
    @DisplayName("체중이 없으면 막는다")
    void 체중이_없으면_막는다() {
        assertThatThrownBy(() -> BreedSize.fromWeight(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private BreedSize size(String weightKg) {
        return BreedSize.fromWeight(new BigDecimal(weightKg));
    }
}
