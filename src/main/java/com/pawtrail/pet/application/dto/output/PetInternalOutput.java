package com.pawtrail.pet.application.dto.output;

import com.pawtrail.pet.domain.enums.BreedSize;
import com.pawtrail.pet.domain.enums.Species;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * 다른 서비스에 내어 주는 반려동물입니다.
 *
 * 공개 조회의 PetOutput 을 재사용하지 않습니다.
 * 사진과 메모를 담지 않기 때문입니다.
 *
 * review 는 후기를 쓸 때 견종과 체중과 크기를 스냅샷으로 복사하고
 * verdict 는 판정 축만 봅니다. 둘 다 사진을 쓰지 않습니다.
 * PetOutput 을 그대로 쓰면 100마리 조회에 서명을 100번 만들게 됩니다.
 * 서명은 통신이 아니라 계산이라 가볍지만, 아무도 안 쓰는 값을 매번 만들 이유가 없습니다.
 *
 * 메모도 담지 않습니다. 보호자가 자기 화면에서 보려고 적은 값입니다.
 *
 * @param breedName         견종 마스터에 없는 코드면 null 입니다.
 *                          breed_code 에 외래 키가 없어 생길 수 있는 상태입니다.
 * @param species           프론트가 안내 문구를 띄울지 가르는 값입니다.
 * @param isDangerousBreed  맹견 여부입니다. verdict 의 breed_rule 판정에 쓰입니다.
 */
public record PetInternalOutput(
        UUID petId,
        String name,
        BigDecimal weightKg,
        BreedSize breedSize,
        boolean hasCarrier,
        boolean hasStroller,
        boolean vaccineCompleted,
        boolean vaccineProofAvailable,

        String breedCode,
        String breedName,
        Species species,
        boolean isDangerousBreed
) {
}
