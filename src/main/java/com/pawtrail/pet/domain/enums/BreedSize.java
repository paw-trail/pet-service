package com.pawtrail.pet.domain.enums;

import java.math.BigDecimal;

/**
 * 반려동물의 크기입니다.
 *
 * 국립축산과학원 「반려견 소개」의 분류를 따릅니다.
 * SMALL 은 10kg 미만, MEDIUM 은 10kg 이상 25kg 미만, LARGE 는 25kg 이상이며
 * 경계값은 10.0kg 이 MEDIUM, 25.0kg 이 LARGE 입니다.
 *
 * 견종 기본값을 보지 않고 체중으로만 가릅니다.
 * 견종별 크기 기본값은 도움이 가장 필요한 믹스견 보호자에게 닿지 않는 반면
 * 체중은 등록할 때 반드시 받는 값이라 누구에게나 채워집니다.
 *
 * 계산 규칙을 여기 두는 이유는 값과 경계가 짝이기 때문입니다.
 * 세 값과 두 경계는 하나를 고치면 반드시 다른 하나도 봐야 하는 관계라,
 * 갈라 두면 값을 늘리고 규칙을 안 고치는 자리가 생깁니다.
 * 등록과 수정 양쪽이 이 메서드를 부릅니다.
 *
 * 그 출처가 스스로 "품종을 구분하는 공식 분류체계가 아니다" 라고 밝히고 있습니다.
 * 화면이 "kg 기준 변환이라 업장 기준과 다를 수 있다" 를 함께 안내하는 근거가 그것입니다.
 */
public enum BreedSize {

    // 10kg 미만
    SMALL,

    // 10kg 이상 25kg 미만
    MEDIUM,

    // 25kg 이상
    LARGE;

    private static final BigDecimal MEDIUM_FROM = new BigDecimal("10.0");
    private static final BigDecimal LARGE_FROM = new BigDecimal("25.0");

    /**
     * 체중에서 크기를 정합니다.
     *
     * 사용자가 크기를 직접 보내면 이 메서드를 부르지 않습니다.
     * 우선순위가 사용자 지정, 그다음이 체중입니다.
     *
     * 값을 compareTo 로 견줍니다.
     * equals 는 자릿수까지 보므로 10 과 10.0 을 다른 값으로 봅니다.
     */
    public static BreedSize fromWeight(BigDecimal weightKg) {
        if (weightKg == null) {
            throw new IllegalArgumentException("weightKg 는 필수입니다.");
        }
        if (weightKg.compareTo(LARGE_FROM) >= 0) {
            return LARGE;
        }
        if (weightKg.compareTo(MEDIUM_FROM) >= 0) {
            return MEDIUM;
        }
        return SMALL;
    }
}
