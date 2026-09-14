package com.pawtrail.pet.domain.model;

import com.pawtrail.common.entity.BaseEntity;
import com.pawtrail.pet.domain.enums.Species;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 견종 마스터입니다.
 *
 * 쓰이는 곳이 셋뿐입니다.
 * 드롭다운에 보일 이름, 맹견 판정, 종 구분입니다.
 * 크기 기본값은 두지 않습니다. 크기를 체중으로만 가르기로 해 읽는 곳이 없어졌습니다.
 *
 * 값은 V21 마이그레이션이 넣고 애플리케이션은 읽기만 합니다.
 * 그래서 생성 팩터리를 두지 않습니다.
 * 견종을 더하거나 고치는 일은 다음 번호의 마이그레이션으로 합니다.
 */
@Entity
@Table(name = "breed")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Breed extends BaseEntity {

    // 사람이 정한 고정 코드임
    // 대리 키를 두지 않는 이유는 화면과 API 가 이 문자열을 그대로 주고받기 때문임
    @Id
    @Column(name = "code", length = 30, nullable = false, updatable = false)
    private String code;

    @Column(name = "name_ko", length = 40, nullable = false)
    private String nameKo;

    // 동물보호법 시행규칙 제2조의 맹견 5종만 참임
    // MIX 와 OTHER 는 잡종을 판정할 수 없어 거짓으로 두고 화면 안내로 보완함
    @Column(name = "is_dangerous", nullable = false)
    private boolean dangerous;

    // 값이 늘 수 있어 컬럼에 CHECK 를 걸지 않았으므로 여기서 이름으로 담음
    @Enumerated(EnumType.STRING)
    @Column(name = "species", length = 12, nullable = false)
    private Species species;
}
