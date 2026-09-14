package com.pawtrail.pet.infrastructure.persistence.jpa;

import com.pawtrail.pet.domain.model.Breed;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 스프링 데이터가 구현을 만들어 주는 인터페이스입니다.
 *
 * 식별자 타입이 UUID 가 아니라 String 입니다. 견종 코드가 PK 이기 때문입니다.
 */
public interface BreedJpaRepository extends JpaRepository<Breed, String> {
}
