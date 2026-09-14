package com.pawtrail.pet.infrastructure.persistence.jpa;

import com.pawtrail.pet.domain.model.Pet;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 스프링 데이터가 구현을 만들어 주는 인터페이스입니다.
 *
 * 메서드 이름이 곧 질의이므로 이름을 바꿀 때 질의가 함께 바뀝니다.
 */
public interface PetJpaRepository extends JpaRepository<Pet, UUID> {

    List<Pet> findAllByAccountIdOrderByCreatedAtAsc(UUID accountId);

    List<Pet> findAllByIdIn(Collection<UUID> ids);
}
