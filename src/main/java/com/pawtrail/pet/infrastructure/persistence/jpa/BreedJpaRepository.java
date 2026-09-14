package com.pawtrail.pet.infrastructure.persistence.jpa;

import com.pawtrail.pet.domain.model.Breed;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 스프링 데이터가 구현을 만들어 주는 인터페이스입니다.
 *
 * 식별자 타입이 UUID 가 아니라 String 입니다. 견종 코드가 PK 이기 때문입니다.
 *
 * 드롭다운 정렬을 질의로 하지 않습니다.
 * ORDER BY 를 쓰면 한글 순서를 데이터베이스 콜레이션이 정하는데 환경마다 다릅니다.
 * 자세한 것은 BreedRepositoryImpl 에 적어 두었습니다.
 */
public interface BreedJpaRepository extends JpaRepository<Breed, String> {
}
