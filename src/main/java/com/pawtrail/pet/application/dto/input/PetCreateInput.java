package com.pawtrail.pet.application.dto.input;

import com.pawtrail.pet.domain.enums.BreedSize;
import java.math.BigDecimal;

/**
 * 반려동물 등록에 필요한 값입니다.
 *
 * 요청 객체를 서비스까지 들고 가지 않습니다.
 * 그러면 응용 계층이 표현 계층의 검증 애노테이션까지 함께 알게 됩니다.
 *
 * @param breedSize  사용자가 고른 크기입니다. 비어 있으면 서비스가 체중에서 계산합니다.
 * @param photoUrl   업로드 응답의 fileUrl 입니다. 서비스가 우리 것인지 대조합니다.
 */
public record PetCreateInput(
        String name,
        String breedCode,
        BigDecimal weightKg,
        BreedSize breedSize,
        boolean hasCarrier,
        boolean hasStroller,
        boolean vaccineCompleted,
        boolean vaccineProofAvailable,
        String photoUrl,
        String note
) {
}
