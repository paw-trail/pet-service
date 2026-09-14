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

    /**
     * 반려동물을 지웁니다.
     *
     * 하드 딜리트입니다.
     * BaseEntity 를 상속하지만 deleted_at 과 deleted_by 는 영영 null 로 남습니다.
     *
     * 소프트로 두지 않는 이유가 둘입니다.
     * 소프트의 근거인 "신원을 끊되 추적은 남긴다" 가 여기서는 서지 않습니다.
     * 반려동물은 신원 주체가 아니고, 후기에 견종과 체중이 이미 스냅샷으로 복사돼 있어
     * 행이 남아도 더 알 것이 없습니다.
     * 그리고 행이 남으면 사진을 언제 지울지가 애매해집니다.
     *
     * favorite 과 visit_log 도 같은 이유로 하드입니다.
     */
    void delete(Pet pet);
}
