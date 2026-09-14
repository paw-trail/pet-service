package com.pawtrail.pet.presentation.request;

import com.pawtrail.pet.domain.enums.BreedSize;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 반려동물 등록 요청입니다.
 *
 * 회원가입 Step 2 와 반려동물 정보 수정 화면의 등록 폼이 보냅니다.
 *
 * @param name                   이름입니다. 최소 길이를 걸지 않습니다.
 *                               "콩" 처럼 한 글자 이름이 실제로 흔합니다.
 * @param breedCode              견종 코드입니다. 드롭다운에서 고른 값입니다.
 *                               있는 코드인지는 서비스가 확인합니다. 외래 키를 걸지 않았습니다.
 * @param weightKg               체중입니다.
 *                               상한 200 은 최대 견종이 90kg 안팎이라 두 배 여유이며,
 *                               800.0 같은 오타를 막는 자리입니다.
 *                               자릿수를 함께 거는 것은 스케일이 넘으면 데이터베이스가
 *                               반올림해 값이 조용히 달라지기 때문입니다.
 * @param breedSize              크기입니다. 선택입니다.
 *                               보내면 그 값을 쓰고, 없으면 체중에서 계산합니다.
 * @param hasCarrier             이동장을 가지고 있는지입니다.
 * @param hasStroller            유모차를 가지고 있는지입니다.
 * @param vaccineCompleted       접종을 마쳤는지입니다.
 * @param vaccineProofAvailable  증명서를 가지고 있는지입니다.
 *                               파일은 받지 않고 보유 여부만 담습니다.
 * @param photoUrl               사진 주소입니다. 선택입니다.
 *                               upload-url 로 받은 fileUrl 을 그대로 보냅니다.
 *                               서버가 우리 버킷의 내 자리인지 대조한 뒤 키만 꺼내 저장합니다.
 * @param note                   메모입니다.
 */
public record PetCreateRequest(

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 30, message = "이름은 30자를 넘을 수 없습니다.")
        String name,

        @NotBlank(message = "견종은 필수입니다.")
        @Size(max = 30, message = "견종 코드가 올바르지 않습니다.")
        String breedCode,

        @NotNull(message = "체중은 필수입니다.")
        @DecimalMin(value = "0.0", inclusive = false, message = "체중은 0보다 커야 합니다.")
        @DecimalMax(value = "200.0", message = "체중은 200kg 을 넘을 수 없습니다.")
        @Digits(integer = 3, fraction = 1, message = "체중은 소수점 첫째 자리까지만 적을 수 있습니다.")
        BigDecimal weightKg,

        BreedSize breedSize,

        // 네 값을 Boolean 으로 받음
        // primitive 로 두면 안 보낸 것이 false 가 되어 "안 보냄" 과 "없음" 이 구분되지 않음
        @NotNull(message = "이동장 보유 여부는 필수입니다.")
        Boolean hasCarrier,

        @NotNull(message = "유모차 보유 여부는 필수입니다.")
        Boolean hasStroller,

        @NotNull(message = "접종 여부는 필수입니다.")
        Boolean vaccineCompleted,

        @NotNull(message = "증명서 보유 여부는 필수입니다.")
        Boolean vaccineProofAvailable,

        String photoUrl,

        @Size(max = 200, message = "메모는 200자를 넘을 수 없습니다.")
        String note
) {
}
