package com.pawtrail.pet.infrastructure.persistence.jpa;

import com.pawtrail.pet.domain.model.Pet;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 스프링 데이터가 구현을 만들어 주는 인터페이스입니다.
 *
 * 메서드 이름이 곧 질의이므로 이름을 바꿀 때 질의가 함께 바뀝니다.
 */
public interface PetJpaRepository extends JpaRepository<Pet, UUID> {

    List<Pet> findAllByAccountIdOrderByCreatedAtAsc(UUID accountId);

    List<Pet> findAllByIdIn(Collection<UUID> ids);

    List<Pet> findAllByAccountId(UUID accountId);

    /**
     * 그 계정의 반려동물을 한 문장으로 지웁니다.
     *
     * flushAutomatically 를 켭니다.
     * 벌크 쿼리는 영속성 컨텍스트를 우회하므로, 앞에서 바꾼 것이 아직 안 나갔으면
     * 지운 뒤에 그 변경이 다시 나가 순서가 뒤집힙니다.
     *
     * clearAutomatically 는 켜지 않습니다.
     * 켜면 영속성 컨텍스트가 통째로 비워져 앞에서 읽어 둔 엔티티가 준영속이 됩니다.
     * 그 변경이 오류 없이 사라지므로 "지웠다고 로그는 찍혔는데 값은 그대로" 가 됩니다.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from Pet p where p.accountId = :accountId")
    int deleteAllByAccountId(@Param("accountId") UUID accountId);
}
