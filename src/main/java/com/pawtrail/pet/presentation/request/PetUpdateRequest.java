package com.pawtrail.pet.presentation.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pawtrail.pet.application.dto.input.PetUpdateInput;
import com.pawtrail.pet.domain.enums.BreedSize;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 반려동물 수정 요청입니다.
 *
 * 등록 요청과 달리 record 가 아닙니다.
 *
 * PATCH 는 "보낸 것만 바꾼다" 가 계약이라
 * 필드를 아예 안 보낸 것과 null 을 보낸 것을 갈라야 합니다.
 * record 로 받으면 둘 다 null 이 되어 구분할 수 없고,
 * 그러면 사진만 바꾸려고 보낸 요청이 이름까지 지웁니다.
 *
 * Jackson 은 JSON 에 그 키가 있을 때만 세터를 부릅니다.
 * 그래서 세터 안에서 플래그를 세우면 별도 라이브러리 없이 세 상태가 갈립니다.
 * Optional 로 받는 방법도 있으나 Jackson 이 "없음" 과 "명시적 null" 을
 * 둘 다 Optional.empty() 로 만들 수 있어 확실하지 않습니다.
 *
 * 세 상태가 이렇게 갈립니다.
 *   키가 없음          provided 가 false         →  그대로 둠
 *   "필드": null      provided 가 true, 값 null  →  아래 규칙대로
 *   "필드": 값         provided 가 true, 값 있음  →  바꿈
 *
 * null 의 뜻이 필드마다 다릅니다.
 *   photoUrl · note   지웁니다
 *   나머지 여덟        400 입니다. 반려동물이 있는 한 반드시 값이 있어야 하는 것들입니다.
 *
 * user-service 는 지울 수 없는 필드가 하나라 검증도 하나였습니다.
 * 여기는 여덟이라 검증 메서드를 여덟 개 두는 대신 하나로 묶었습니다.
 * 어느 필드가 걸렸는지는 응답 data 에 담기지 않지만,
 * 이 요청은 어떤 정상 경로로도 오지 않으므로 프론트 버그이거나 조작입니다.
 */
@Getter
@NoArgsConstructor
public class PetUpdateRequest {

    @Size(max = 30, message = "이름은 30자를 넘을 수 없습니다.")
    private String name;

    private boolean nameProvided;

    @Size(max = 30, message = "견종 코드가 올바르지 않습니다.")
    private String breedCode;

    private boolean breedCodeProvided;

    @DecimalMin(value = "0.0", inclusive = false, message = "체중은 0보다 커야 합니다.")
    @DecimalMax(value = "200.0", message = "체중은 200kg 을 넘을 수 없습니다.")
    @Digits(integer = 3, fraction = 1, message = "체중은 소수점 첫째 자리까지만 적을 수 있습니다.")
    private BigDecimal weightKg;

    private boolean weightKgProvided;

    private BreedSize breedSize;

    private boolean breedSizeProvided;

    private Boolean hasCarrier;

    private boolean hasCarrierProvided;

    private Boolean hasStroller;

    private boolean hasStrollerProvided;

    private Boolean vaccineCompleted;

    private boolean vaccineCompletedProvided;

    private Boolean vaccineProofAvailable;

    private boolean vaccineProofAvailableProvided;

    // 아래 둘만 null 이 "지운다" 는 뜻임
    private String photoUrl;

    private boolean photoUrlProvided;

    @Size(max = 200, message = "메모는 200자를 넘을 수 없습니다.")
    private String note;

    private boolean noteProvided;

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
        this.nameProvided = true;
    }

    @JsonProperty("breedCode")
    public void setBreedCode(String breedCode) {
        this.breedCode = breedCode;
        this.breedCodeProvided = true;
    }

    @JsonProperty("weightKg")
    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
        this.weightKgProvided = true;
    }

    @JsonProperty("breedSize")
    public void setBreedSize(BreedSize breedSize) {
        this.breedSize = breedSize;
        this.breedSizeProvided = true;
    }

    @JsonProperty("hasCarrier")
    public void setHasCarrier(Boolean hasCarrier) {
        this.hasCarrier = hasCarrier;
        this.hasCarrierProvided = true;
    }

    @JsonProperty("hasStroller")
    public void setHasStroller(Boolean hasStroller) {
        this.hasStroller = hasStroller;
        this.hasStrollerProvided = true;
    }

    @JsonProperty("vaccineCompleted")
    public void setVaccineCompleted(Boolean vaccineCompleted) {
        this.vaccineCompleted = vaccineCompleted;
        this.vaccineCompletedProvided = true;
    }

    @JsonProperty("vaccineProofAvailable")
    public void setVaccineProofAvailable(Boolean vaccineProofAvailable) {
        this.vaccineProofAvailable = vaccineProofAvailable;
        this.vaccineProofAvailableProvided = true;
    }

    @JsonProperty("photoUrl")
    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
        this.photoUrlProvided = true;
    }

    @JsonProperty("note")
    public void setNote(String note) {
        this.note = note;
        this.noteProvided = true;
    }

    /**
     * 지울 수 없는 값을 지우려는 요청을 막습니다.
     *
     * 화면에 그 여덟을 비우는 자리가 없고, 등록할 때 전부 필수로 받은 값들입니다.
     * 어떤 정상 경로로도 오지 않으므로 오면 프론트 버그이거나 조작입니다.
     *
     * 지우게 두면 체중이 빈 반려동물이 생기는데,
     * 크기를 체중에서 계산하므로 판정의 입력이 통째로 사라집니다.
     *
     * photoUrl 과 note 는 대칭이 아닙니다.
     * 사진이 없으면 기본 이미지가 뜨고 메모는 원래 선택이라 화면이 성립합니다.
     */
    @AssertTrue(message = "이름 · 견종 · 체중 · 크기 · 동반 장비 · 접종 정보는 지울 수 없습니다")
    public boolean isRequiredFieldsNotCleared() {
        return notCleared(nameProvided, name)
                && notCleared(breedCodeProvided, breedCode)
                && notCleared(weightKgProvided, weightKg)
                && notCleared(breedSizeProvided, breedSize)
                && notCleared(hasCarrierProvided, hasCarrier)
                && notCleared(hasStrollerProvided, hasStroller)
                && notCleared(vaccineCompletedProvided, vaccineCompleted)
                && notCleared(vaccineProofAvailableProvided, vaccineProofAvailable);
    }

    /**
     * 사진 주소가 빈 문자열인 요청을 막습니다.
     *
     * 계약은 명시적 null 만 "지운다" 로 정했습니다.
     * 그런데 서비스가 빈 값을 null 로 바꿔 저장하므로,
     * 막지 않으면 빈 문자열도 사진을 지우고 옛 S3 객체까지 함께 지웁니다.
     *
     * 프론트 폼이 빈 입력을 "" 로 보내기 쉬운 자리입니다.
     * 사진을 건드리지 않았는데 초기값이 빈 문자열이면 사진이 사라지고,
     * 객체까지 지워져 되돌릴 수도 없습니다.
     *
     * 메모는 막지 않습니다. 빈 문자열과 null 이 사실상 같은 뜻이고 지워도 해롭지 않습니다.
     */
    @AssertTrue(message = "사진 주소는 비워 보낼 수 없습니다. 지우려면 null 을 보냅니다")
    public boolean isPhotoUrlNotBlank() {
        return !photoUrlProvided || photoUrl == null || !photoUrl.isBlank();
    }

    /**
     * 이름이 빈 문자열인 요청을 막습니다.
     *
     * @Size 는 길이만 보므로 "" 가 통과합니다.
     * 등록에서는 @NotBlank 가 막고 있는 자리인데 수정에는 그 애노테이션을 걸 수 없습니다.
     * 안 보낸 요청까지 막아 버리기 때문입니다.
     */
    @AssertTrue(message = "이름은 공백일 수 없습니다")
    public boolean isNameNotBlank() {
        return !nameProvided || name == null || !name.isBlank();
    }

    private boolean notCleared(boolean provided, Object value) {
        return !provided || value != null;
    }

    /**
     * 서비스가 받는 형태로 바꿉니다.
     *
     * 플래그를 전부 넘깁니다.
     * 지울 수 없는 여덟은 명시적 null 이 위 검증에서 막히므로
     * 플래그가 곧 "바꾼다" 는 뜻이 되고,
     * photoUrl 과 note 는 플래그와 값을 함께 봐야 "지운다" 를 표현할 수 있습니다.
     *
     * 계정 식별자와 반려동물 식별자는 담지 않습니다.
     * 앞은 게이트웨이 헤더에서, 뒤는 경로에서 오므로 컨트롤러가 따로 넘깁니다.
     */
    public PetUpdateInput toInput() {
        return new PetUpdateInput(
                nameProvided, name,
                breedCodeProvided, breedCode,
                weightKgProvided, weightKg,
                breedSizeProvided, breedSize,
                hasCarrierProvided, hasCarrier,
                hasStrollerProvided, hasStroller,
                vaccineCompletedProvided, vaccineCompleted,
                vaccineProofAvailableProvided, vaccineProofAvailable,
                photoUrlProvided, photoUrl,
                noteProvided, note);
    }
}
