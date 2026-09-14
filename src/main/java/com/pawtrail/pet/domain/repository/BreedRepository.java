package com.pawtrail.pet.domain.repository;

import com.pawtrail.pet.domain.model.Breed;
import java.util.Optional;

/**
 * 견종 마스터 저장소입니다.
 *
 * 읽기만 있습니다. 값은 마이그레이션이 넣습니다.
 *
 * 목록 조회는 아직 두지 않습니다.
 * 드롭다운에 보일 순서를 정해야 하는데 MIX 와 OTHER 를 맨 끝에 두기로 해서
 * 단순 정렬로는 표현되지 않습니다. 견종 조회 API 와 함께 정합니다.
 */
public interface BreedRepository {

    /**
     * 견종 코드로 찾습니다.
     *
     * 반려동물을 등록할 때 이 결과로 두 가지를 합니다.
     * 없으면 400 으로 막고, 있으면 species 와 맹견 여부를 채웁니다.
     * 외래 키를 걸지 않는 대신 이 조회가 그 역할을 합니다.
     */
    Optional<Breed> findByCode(String code);
}
