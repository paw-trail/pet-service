package com.pawtrail.pet.application.service;

import com.pawtrail.pet.application.dto.output.BreedOutput;
import com.pawtrail.pet.domain.repository.BreedRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 견종 마스터를 읽습니다.
 *
 * 쓰기가 없어 이름에 Query 를 붙이지 않았습니다.
 * 값이 마이그레이션으로만 바뀌므로 쓰기 서비스가 생길 일이 없고,
 * 가를 짝이 없으면 접미사가 정보를 늘리지 않습니다.
 *
 * 캐시를 두지 않습니다.
 * 45행 조회라 비용이 사실상 없고, 캐시를 붙이면 의존성과 설정만 늘어납니다.
 */
@Service
@RequiredArgsConstructor
public class BreedService {

    private final BreedRepository breedRepository;

    /**
     * 드롭다운에 보일 견종을 전부 돌려줍니다.
     *
     * 순서는 저장소가 정합니다. 이름 가나다순이되 MIX 와 OTHER 가 맨 끝입니다.
     */
    @Transactional(readOnly = true)
    public List<BreedOutput> getBreeds() {
        return breedRepository.findAllForDropdown().stream()
                .map(breed -> new BreedOutput(breed.getCode(), breed.getNameKo()))
                .toList();
    }
}
