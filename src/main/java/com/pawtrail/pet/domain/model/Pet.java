package com.pawtrail.pet.domain.model;

import com.pawtrail.common.entity.BaseEntity;
import com.pawtrail.pet.domain.enums.BreedSize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * 반려동물입니다.
 *
 * 이 엔티티의 필드가 곧 판정 규칙의 입력입니다.
 * verdict 가 체중 · 크기 · 장비 · 접종 · 견종을 읽어 동반 가능 여부를 냅니다.
 *
 * 종과 맹견 여부는 필드로 두지 않습니다.
 * 둘 다 breedCode 하나에서 정해지는 값이라 복사해 두면 원본과 어긋나고,
 * 맹견 목록은 시행규칙이 개정되면 바뀌므로 이미 등록된 행이 옛값으로 남습니다.
 * 필요한 곳에서 BreedRepository 로 따로 읽습니다.
 */
@Entity
@Table(name = "pet")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    // 보호자임
    // auth_db 의 값이라 외래 키를 걸지 않음
    // 내어 주는 두 internal API 가 이 값과 게이트웨이의 X-User-Id 를 대조함
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "name", length = 30, nullable = false)
    private String name;

    // breed.code 를 가리킴
    // 같은 DB 안이지만 외래 키를 걸지 않고 서비스가 존재를 확인함
    @Column(name = "breed_code", length = 30, nullable = false)
    private String breedCode;

    // 서버가 체중에서 계산해 채우되 사용자가 고칠 수 있음
    // 유도값인데도 담는 이유는 서버가 채운 값과 사용자가 고친 값을 구분할 수단이 이것뿐이기 때문임
    @Enumerated(EnumType.STRING)
    @Column(name = "breed_size", length = 12, nullable = false)
    private BreedSize breedSize;

    // 크기를 체중에서만 계산하므로 비면 breedSize 를 채울 재료가 없어 필수임
    @Column(name = "weight_kg", nullable = false, precision = 4, scale = 1)
    private BigDecimal weightKg;

    // 목줄로는 안 되고 이동장이 있어야만 들어갈 수 있는 장소가 실재함
    @Column(name = "has_carrier", nullable = false)
    private boolean carrier;

    // 이동장과 유모차 중 하나만 있어도 되는 장소가 있어 따로 받음
    @Column(name = "has_stroller", nullable = false)
    private boolean stroller;

    @Column(name = "vaccine_completed", nullable = false)
    private boolean vaccineCompleted;

    // 접종은 했는데 증명서가 없어 못 들어가는 곳이 있어 두 값을 따로 받음
    // 파일은 받지 않고 보유 여부만 담음
    @Column(name = "vaccine_proof_available", nullable = false)
    private boolean vaccineProofAvailable;

    // S3 주소임
    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "note", length = 200)
    private String note;

    private Pet(UUID accountId, String name, String breedCode, BreedSize breedSize,
            BigDecimal weightKg, boolean carrier, boolean stroller,
            boolean vaccineCompleted, boolean vaccineProofAvailable,
            String photoUrl, String note) {

        this.accountId = accountId;
        this.name = name;
        this.breedCode = breedCode;
        this.breedSize = breedSize;
        this.weightKg = weightKg;
        this.carrier = carrier;
        this.stroller = stroller;
        this.vaccineCompleted = vaccineCompleted;
        this.vaccineProofAvailable = vaccineProofAvailable;
        this.photoUrl = photoUrl;
        this.note = note;
    }

    /**
     * 판정에 쓰이는 값이 이 값들과 다른지 봅니다.
     *
     * 축은 일곱입니다. 체중 · 크기 · 이동장 · 유모차 · 접종 여부 · 증명서 보유 · 견종입니다.
     * 이름과 사진과 메모는 판정에 쓰이지 않으므로 보지 않습니다.
     *
     * 견종이 축인 것은 맹견 판정 때문입니다.
     * 견종이 바뀌면 맹견 여부가 달라져 판정이 뒤집힐 수 있습니다.
     *
     * 체중만 compareTo 로 견줍니다.
     * BigDecimal 의 equals 는 자릿수까지 보아 12 와 12.0 을 다른 값으로 봅니다.
     *
     * 요청에 그 필드가 있었는지가 아니라 값이 실제로 달라졌는지를 봅니다.
     * 화면이 폼 전체를 보내므로 이름만 고쳐도 일곱 축이 전부 실려 오고,
     * 있었는지만 보면 "이름만 바꾸면 거짓" 이라는 설계가 한 번도 작동하지 않습니다.
     */
    public boolean isVerdictRelevantChangedFrom(
            BigDecimal weightKg, BreedSize breedSize, String breedCode,
            boolean carrier, boolean stroller,
            boolean vaccineCompleted, boolean vaccineProofAvailable) {

        return this.weightKg.compareTo(weightKg) != 0
                || this.breedSize != breedSize
                || !this.breedCode.equals(breedCode)
                || this.carrier != carrier
                || this.stroller != stroller
                || this.vaccineCompleted != vaccineCompleted
                || this.vaccineProofAvailable != vaccineProofAvailable;
    }

    /**
     * 보낸 값으로 바꿉니다.
     *
     * 호출하는 쪽이 "안 보낸 것은 지금 값" 으로 이미 채워서 넘깁니다.
     * 여기서 필드마다 null 을 가리면 부분 수정 규칙이 엔티티와 서비스 두 곳에 생깁니다.
     *
     * 사진과 메모는 null 이 "지운다" 는 뜻이라 그대로 받습니다.
     */
    public void update(String name, String breedCode, BigDecimal weightKg, BreedSize breedSize,
            boolean carrier, boolean stroller,
            boolean vaccineCompleted, boolean vaccineProofAvailable,
            String photoUrl, String note) {

        this.name = name;
        this.breedCode = breedCode;
        this.weightKg = weightKg;
        this.breedSize = breedSize;
        this.carrier = carrier;
        this.stroller = stroller;
        this.vaccineCompleted = vaccineCompleted;
        this.vaccineProofAvailable = vaccineProofAvailable;
        this.photoUrl = photoUrl;
        this.note = note;
    }

    /**
     * 반려동물을 등록합니다.
     *
     * breedSize 는 호출하는 쪽이 이미 정한 값을 넘깁니다.
     * 사용자가 보냈으면 그 값이고, 안 보냈으면 체중에서 계산한 값입니다.
     * 그 계산 규칙은 등록 기능과 함께 들어옵니다.
     *
     * 여기서는 없으면 안 되는 값이 비었는지와 체중이 양수인지를 봅니다.
     * 길이와 그 밖의 범위 검증은 요청 객체가 이미 마친 뒤입니다.
     *
     * 체중만 여기서도 보는 이유는 그 값이 크기 계산과 판정의 입력이기 때문입니다.
     * 0 이나 음수가 들어가면 SMALL 로 떨어져 "동반 가능" 이 잘못 나갑니다.
     * 같은 조건이 DB 에도 ck_pet_weight_positive 로 걸려 있습니다.
     */
    public static Pet create(UUID accountId, String name, String breedCode, BreedSize breedSize,
            BigDecimal weightKg, boolean carrier, boolean stroller,
            boolean vaccineCompleted, boolean vaccineProofAvailable,
            String photoUrl, String note) {

        if (accountId == null || name == null || breedCode == null
                || breedSize == null || weightKg == null) {
            throw new IllegalArgumentException(
                    "accountId · name · breedCode · breedSize · weightKg 는 필수입니다.");
        }
        if (weightKg.signum() <= 0) {
            throw new IllegalArgumentException("weightKg 는 0보다 커야 합니다.");
        }
        return new Pet(accountId, name, breedCode, breedSize, weightKg,
                carrier, stroller, vaccineCompleted, vaccineProofAvailable, photoUrl, note);
    }
}
