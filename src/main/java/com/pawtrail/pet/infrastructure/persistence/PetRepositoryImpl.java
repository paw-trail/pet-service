package com.pawtrail.pet.infrastructure.persistence;

import com.pawtrail.pet.domain.model.Pet;
import com.pawtrail.pet.domain.repository.PetRepository;
import com.pawtrail.pet.infrastructure.persistence.jpa.PetJpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 도메인이 선언한 약속을 스프링 데이터로 구현합니다.
 */
@Repository
@RequiredArgsConstructor
public class PetRepositoryImpl implements PetRepository {

    private final PetJpaRepository petJpaRepository;

    @Override
    public Pet save(Pet pet) {
        return petJpaRepository.save(pet);
    }

    @Override
    public Optional<Pet> findById(UUID id) {
        return petJpaRepository.findById(id);
    }

    @Override
    public List<Pet> findAllByAccountIdOrderByCreatedAtAsc(UUID accountId) {
        return petJpaRepository.findAllByAccountIdOrderByCreatedAtAsc(accountId);
    }

    @Override
    public List<Pet> findAllByIdIn(Collection<UUID> ids) {
        return petJpaRepository.findAllByIdIn(ids);
    }

    @Override
    public void delete(Pet pet) {
        petJpaRepository.delete(pet);
    }

    @Override
    public List<Pet> findAllByAccountId(UUID accountId) {
        return petJpaRepository.findAllByAccountId(accountId);
    }

    @Override
    public int deleteAllByAccountId(UUID accountId) {
        return petJpaRepository.deleteAllByAccountId(accountId);
    }
}
