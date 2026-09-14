package com.pawtrail.pet.infrastructure.persistence;

import com.pawtrail.pet.domain.model.Breed;
import com.pawtrail.pet.domain.repository.BreedRepository;
import com.pawtrail.pet.infrastructure.persistence.jpa.BreedJpaRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 도메인이 선언한 약속을 스프링 데이터로 구현합니다.
 */
@Repository
@RequiredArgsConstructor
public class BreedRepositoryImpl implements BreedRepository {

    // 목록 맨 끝에 오는 두 코드임
    // 숫자가 작을수록 앞이고 0 은 그 밖의 모든 견종임
    private static final String MIX = "MIX";
    private static final String OTHER = "OTHER";
    private static final int HEAD = 0;
    private static final int MIX_RANK = 1;
    private static final int OTHER_RANK = 2;

    // 끝 두 줄을 먼저 가르고 그 밖은 이름순으로 세움
    //
    // 정렬을 질의가 아니라 여기서 하는 이유임
    //   ORDER BY 를 쓰면 한글 순서를 데이터베이스 콜레이션이 정하는데 환경마다 다름
    //   ⛔실제로 로컬 pet_db 에서 글자 수가 앞서고 그 안에서만 가나다인 순서가 나왔음
    //     말티즈가 ㅁ 자리가 아니라 세 글자 무리에 끼어 사용자가 찾을 수 없었음
    //   ⛔테스트 컨테이너와 로컬 DB 의 이미지가 달라 콜레이션도 다름
    //     질의에 맡기면 검사를 통과해도 실물이 틀릴 수 있음
    //
    // 자바 문자열의 자연 순서는 코드포인트 순이고
    // 한글 음절이 유니코드에 가나다 순으로 이어져 있어 사전 순과 같아짐
    //
    // 45행짜리 고정 마스터라 메모리에서 세우는 비용을 따로 잴 것이 없음
    private static final Comparator<Breed> DROPDOWN_ORDER =
            Comparator.comparingInt(BreedRepositoryImpl::tailRank)
                    .thenComparing(Breed::getNameKo);

    private final BreedJpaRepository breedJpaRepository;

    @Override
    public Optional<Breed> findByCode(String code) {
        return breedJpaRepository.findById(code);
    }

    @Override
    public List<Breed> findAllForDropdown() {
        return breedJpaRepository.findAll().stream()
                .sorted(DROPDOWN_ORDER)
                .toList();
    }

    private static int tailRank(Breed breed) {
        if (MIX.equals(breed.getCode())) {
            return MIX_RANK;
        }
        if (OTHER.equals(breed.getCode())) {
            return OTHER_RANK;
        }
        return HEAD;
    }
}
