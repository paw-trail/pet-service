package com.pawtrail.pet.application.dto.output;

import com.pawtrail.pet.domain.enums.BreedSize;
import com.pawtrail.pet.domain.enums.Species;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * 반려동물 한 마리입니다.
 *
 * 앞의 열 값은 pet 표에서 오고 뒤의 넷은 견종 마스터를 조인해 채웁니다.
 * 그 넷을 pet 표에 복사해 두지 않는 이유는 견종 코드 하나에서 정해지는 값이고,
 * 특히 맹견 목록은 시행규칙이 개정되면 바뀌어 복사본이 옛값으로 남기 때문입니다.
 *
 * isDangerousBreed 를 담습니다. 견종 드롭다운(GET /breeds)에서는 뺐던 값입니다.
 * 거기는 고르기 전이라 노출하면 낙인이 되지만, 등록된 반려동물은 이미 내 개이고
 * 맹견이라 입마개가 필요한 곳이 있다는 것을 보호자가 알아야 합니다.
 *
 * species 는 프론트가 안내 문구를 띄울지 가르는 값입니다.
 * 종이 개가 아니어도 판정은 개 기준으로 그대로 수행하고,
 * 장소 상세 상단에만 "이 기준은 강아지 기준이라 다를 수 있습니다" 를 한 줄 띄웁니다.
 *
 * photoUrl 은 서명된 주소입니다. 버킷이 퍼블릭 액세스를 막아 두었기 때문입니다.
 * 정해진 시간 뒤 만료되므로 이 응답을 캐시하면 깨진 이미지가 됩니다.
 * 사진이 없으면 null 입니다.
 */
public record PetOutput(
        UUID petId,
        String name,
        BigDecimal weightKg,
        BreedSize breedSize,
        boolean hasCarrier,
        boolean hasStroller,
        boolean vaccineCompleted,
        boolean vaccineProofAvailable,
        String photoUrl,
        String note,

        String breedCode,
        String breedName,
        Species species,
        boolean isDangerousBreed
) {
}
