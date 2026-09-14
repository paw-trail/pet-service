package com.pawtrail.pet.application.dto.input;

import com.pawtrail.pet.domain.enums.BreedSize;
import java.math.BigDecimal;

/**
 * 반려동물 수정에 필요한 값입니다.
 *
 * 필드마다 "보냈는가" 와 값이 짝으로 옵니다.
 * PATCH 가 "보낸 것만 바꾼다" 이므로 값만으로는 안 보낸 것과 지우려는 것이 구분되지 않습니다.
 *
 * 지울 수 있는 것은 photoUrl 과 note 뿐입니다.
 * 나머지 여덟은 명시적 null 이 요청 계층에서 이미 막혔으므로
 * 플래그가 참이면 값이 반드시 있습니다.
 */
public record PetUpdateInput(
        boolean nameProvided, String name,
        boolean breedCodeProvided, String breedCode,
        boolean weightKgProvided, BigDecimal weightKg,
        boolean breedSizeProvided, BreedSize breedSize,
        boolean hasCarrierProvided, Boolean hasCarrier,
        boolean hasStrollerProvided, Boolean hasStroller,
        boolean vaccineCompletedProvided, Boolean vaccineCompleted,
        boolean vaccineProofAvailableProvided, Boolean vaccineProofAvailable,
        boolean photoUrlProvided, String photoUrl,
        boolean noteProvided, String note
) {
}
