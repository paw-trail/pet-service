package com.pawtrail.pet.domain.repository;

import com.pawtrail.pet.domain.model.Breed;
import java.util.List;
import java.util.Optional;

/**
 * 견종 마스터 저장소입니다.
 *
 * 읽기만 있습니다. 값은 마이그레이션이 넣습니다.
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

    /**
     * 드롭다운에 보일 순서로 전부 돌려줍니다.
     *
     * 이름 가나다순이되 끝에 MIX 를, 맨 뒤에 OTHER 를 둡니다.
     * 그 둘은 "내 개가 목록에 없을 때 고르는 자리" 라서
     * 이름 순서에 섞이면 중간에 묻힙니다.
     *
     * 둘 사이의 순서도 정해져 있습니다.
     * 믹스를 먼저 보여야 개를 기르는 사람이 "그 외" 를 고르는 일이 줄어듭니다.
     *
     * 45행짜리 고정 마스터라 페이징하지 않습니다.
     */
    List<Breed> findAllForDropdown();
}
