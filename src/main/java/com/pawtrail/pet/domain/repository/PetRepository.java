package com.pawtrail.pet.domain.repository;

import com.pawtrail.pet.domain.model.Pet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 반려동물 저장소입니다.
 *
 * 도메인이 필요한 것만 선언하고 구현은 infrastructure 가 맡습니다.
 * 스프링 데이터 인터페이스를 도메인에 그대로 두지 않는 이유는
 * 쓰지 않는 메서드 수십 개가 함께 열려 어디까지가 약속인지 흐려지기 때문입니다.
 */
public interface PetRepository {

    Pet save(Pet pet);

    Optional<Pet> findById(UUID id);

    /**
     * 한 보호자의 반려동물을 등록한 순서대로 돌려줍니다.
     *
     * 대표 반려동물을 앞세우지 않습니다.
     * 대표가 누구인지는 user 가 가진 값이라 이 서비스가 알지 못합니다.
     */
    List<Pet> findAllByAccountIdOrderByCreatedAtAsc(UUID accountId);

    /**
     * 식별자 여럿으로 한 번에 찾습니다.
     *
     * review 가 후기를 쓸 때 견종과 체중을 스냅샷으로 복사해 가는 경로가 이것입니다.
     * 없는 식별자는 결과에서 빠질 뿐 오류가 아닙니다.
     */
    List<Pet> findAllByIdIn(Collection<UUID> ids);
}
