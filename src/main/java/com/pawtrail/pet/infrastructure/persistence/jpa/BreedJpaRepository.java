package com.pawtrail.pet.infrastructure.persistence.jpa;

import com.pawtrail.pet.domain.model.Breed;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 스프링 데이터가 구현을 만들어 주는 인터페이스입니다.
 *
 * 식별자 타입이 UUID 가 아니라 String 입니다. 견종 코드가 PK 이기 때문입니다.
 */
public interface BreedJpaRepository extends JpaRepository<Breed, String> {

    /**
     * 이름 가나다순이되 끝에 MIX 를, 그 뒤에 OTHER 를 둡니다.
     *
     * 두 코드에 순번을 따로 줍니다.
     * 한 덩어리로 묶어 같은 순번을 주면 그다음 기준인 이름순으로 갈리는데,
     * "그 외 (고양이 등)" 이 "믹스 · 목록에 없는 견종" 보다 앞서므로 순서가 뒤집힙니다.
     *
     * 정렬을 위한 컬럼을 따로 두지 않습니다.
     * 인기순으로 두려면 순위를 데이터에 박아야 하는데 목록 대부분이 출처 있는 순위가 아니고,
     * 견종을 중간에 추가할 때마다 값을 다시 매겨야 합니다.
     */
    @Query("""
            SELECT b FROM Breed b
            ORDER BY CASE b.code WHEN 'MIX' THEN 1 WHEN 'OTHER' THEN 2 ELSE 0 END, b.nameKo
            """)
    List<Breed> findAllForDropdown();
}
