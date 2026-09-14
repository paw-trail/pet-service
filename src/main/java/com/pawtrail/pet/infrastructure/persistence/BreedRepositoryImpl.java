package com.pawtrail.pet.infrastructure.persistence;

import com.pawtrail.pet.domain.model.Breed;
import com.pawtrail.pet.domain.repository.BreedRepository;
import com.pawtrail.pet.infrastructure.persistence.jpa.BreedJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 도메인이 선언한 약속을 스프링 데이터로 구현합니다.
 */
@Repository
@RequiredArgsConstructor
public class BreedRepositoryImpl implements BreedRepository {

    private final BreedJpaRepository breedJpaRepository;

    @Override
    public Optional<Breed> findByCode(String code) {
        return breedJpaRepository.findById(code);
    }
}
