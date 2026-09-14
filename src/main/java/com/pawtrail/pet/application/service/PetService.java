package com.pawtrail.pet.application.service;

import com.pawtrail.common.exception.CommonErrorCode;
import com.pawtrail.common.exception.CustomException;
import com.pawtrail.common.message.outbox.OutboxEventRecorder;
import com.pawtrail.pet.application.dto.input.PetCreateInput;
import com.pawtrail.pet.application.dto.input.PetUpdateInput;
import com.pawtrail.pet.application.support.AfterCommitExecutor;
import com.pawtrail.pet.application.dto.output.PetOutput;
import com.pawtrail.pet.application.dto.output.UploadUrlOutput;
import com.pawtrail.pet.domain.enums.BreedSize;
import com.pawtrail.pet.domain.event.payload.PetProfileUpdatedEvent;
import com.pawtrail.pet.domain.exception.PetErrorCode;
import com.pawtrail.pet.domain.model.Breed;
import com.pawtrail.pet.domain.model.Pet;
import com.pawtrail.pet.domain.provider.StorageProvider;
import com.pawtrail.pet.domain.repository.BreedRepository;
import com.pawtrail.pet.domain.repository.PetRepository;
import com.pawtrail.pet.infrastructure.config.StorageProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 반려동물을 등록하고 조회합니다.
 *
 * 누구의 반려동물인지는 요청에서 받지 않습니다.
 * 게이트웨이가 토큰을 검증해 X-User-Id 로 넣어 준 값을 씁니다.
 *
 * 수정과 삭제는 다음 이슈입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final BreedRepository breedRepository;
    private final StorageProvider storageProvider;
    private final StorageProperties storageProperties;
    private final OutboxEventRecorder outboxEventRecorder;
    private final AfterCommitExecutor afterCommitExecutor;

    /**
     * 반려동물을 등록합니다.
     *
     * 크기를 정하는 순서가 둘입니다.
     * 요청에 왔으면 그 값을 쓰고, 없으면 체중에서 계산합니다.
     * 견종 기본값은 보지 않습니다.
     *
     * 견종이 있는 코드인지 여기서 확인합니다.
     * 외래 키를 걸지 않았기 때문이며, 어차피 종과 맹견 여부를 채우려고 읽어야 해서
     * 확인에 드는 비용이 따로 없습니다.
     */
    @Transactional
    public PetOutput create(UUID accountId, PetCreateInput input) {
        Breed breed = breedRepository.findByCode(input.breedCode())
                .orElseThrow(() -> {
                    log.warn("없는 견종 코드입니다: accountId={}, breedCode={}",
                            accountId, input.breedCode());
                    return new CustomException(CommonErrorCode.VALIDATION_FAILED);
                });

        BreedSize breedSize = input.breedSize() != null
                ? input.breedSize()
                : BreedSize.fromWeight(input.weightKg());

        Pet pet = petRepository.save(Pet.create(
                accountId,
                input.name(),
                input.breedCode(),
                breedSize,
                input.weightKg(),
                input.hasCarrier(),
                input.hasStroller(),
                input.vaccineCompleted(),
                input.vaccineProofAvailable(),
                toStoredKey(accountId, input.photoUrl()),
                input.note()));

        log.info("반려동물을 등록했습니다: accountId={}, petId={}", accountId, pet.getId());

        return toOutput(pet, breed);
    }

    /**
     * 내 반려동물을 전부 봅니다.
     *
     * 견종을 반려동물마다 읽지 않고 코드를 모아 한 번에 읽습니다.
     * 지금은 한 사람이 한두 마리라 차이가 작지만, 이 조립이
     * GET /internal/pets?ids= 에서 그대로 쓰이고 그쪽은 상한이 100 입니다.
     */
    @Transactional(readOnly = true)
    public List<PetOutput> getMyPets(UUID accountId) {
        List<Pet> pets = petRepository.findAllByAccountIdOrderByCreatedAtAsc(accountId);
        if (pets.isEmpty()) {
            return List.of();
        }

        Map<String, Breed> breeds = loadBreeds(pets);

        return pets.stream()
                .map(pet -> toOutput(pet, breeds.get(pet.getBreedCode())))
                .toList();
    }

    /**
     * 반려동물 하나를 봅니다.
     *
     * 없는 경우와 남의 것인 경우에 같은 응답을 냅니다.
     * 403 을 주면 "그 반려동물은 있는데 네 것이 아니다" 를 알려 주는 셈이 됩니다.
     */
    @Transactional(readOnly = true)
    public PetOutput getPet(UUID accountId, UUID petId) {
        Pet pet = getOwnedOrThrow(accountId, petId);

        Breed breed = breedRepository.findByCode(pet.getBreedCode()).orElse(null);

        return toOutput(pet, breed);
    }

    /**
     * 사진을 올릴 주소를 발급합니다.
     *
     * 파일은 이 서버를 거치지 않습니다. 브라우저가 S3 로 직접 PUT 합니다.
     *
     * 크기 상한을 여기서 봅니다.
     * 서명에 크기가 들어가 있어 S3 도 그 크기가 아니면 거부하지만,
     * 그것은 "요청한 크기와 다른 것" 을 막을 뿐 상한을 막지는 못합니다.
     * 형식은 요청 객체가 이미 걸렀습니다.
     */
    public UploadUrlOutput issueUploadUrl(
            UUID accountId, String contentType, long contentLength) {

        if (contentLength > storageProperties.maxImageBytes()) {
            log.warn("이미지가 상한을 넘었습니다: accountId={}, contentLength={}, max={}",
                    accountId, contentLength, storageProperties.maxImageBytes());
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }

        String key = storageProvider.newPhotoKey(accountId);

        return new UploadUrlOutput(
                storageProvider.presignUpload(key, contentType, contentLength),
                storageProvider.publicUrl(key),
                storageProperties.uploadExpiresSeconds());
    }

    /**
     * 반려동물을 고칩니다.
     *
     * 보낸 것만 바꿉니다. 안 보낸 필드는 지금 값을 그대로 둡니다.
     * 명시적 null 로 지울 수 있는 것은 사진과 메모뿐이며, 나머지는 요청 계층이 막았습니다.
     *
     * 체중이 바뀌었는데 크기를 안 보냈으면 크기를 다시 계산합니다.
     * 안 그러면 체중을 12 에서 30 으로 고쳐도 크기가 SMALL 로 남아
     * 판정이 틀린 값으로 나갑니다.
     *
     * 값이 하나도 안 바뀌었으면 이벤트를 발행하지 않습니다.
     * 이벤트의 뜻이 "네가 가진 것이 낡았다" 인데 안 바뀌었으면 낡지 않았습니다.
     *
     * 사진을 바꾸거나 지우면 옛 객체를 지웁니다.
     * 커밋 뒤에 지우는 이유는 AfterCommitExecutor 에 적어 두었습니다.
     */
    @Transactional
    public PetOutput update(UUID accountId, UUID petId, PetUpdateInput input) {
        Pet pet = getOwnedOrThrow(accountId, petId);

        String name = input.nameProvided() ? input.name() : pet.getName();
        String breedCode = input.breedCodeProvided() ? input.breedCode() : pet.getBreedCode();
        BigDecimal weightKg = input.weightKgProvided() ? input.weightKg() : pet.getWeightKg();

        // 크기를 정하는 순서가 등록과 같음
        // 보냈으면 그 값, 아니면 체중에서 계산, 체중도 안 바뀌었으면 지금 값
        BreedSize breedSize;
        if (input.breedSizeProvided()) {
            breedSize = input.breedSize();
        } else if (input.weightKgProvided()) {
            breedSize = BreedSize.fromWeight(weightKg);
        } else {
            breedSize = pet.getBreedSize();
        }

        boolean carrier = input.hasCarrierProvided() ? input.hasCarrier() : pet.isCarrier();
        boolean stroller = input.hasStrollerProvided() ? input.hasStroller() : pet.isStroller();
        boolean vaccineCompleted = input.vaccineCompletedProvided()
                ? input.vaccineCompleted() : pet.isVaccineCompleted();
        boolean vaccineProofAvailable = input.vaccineProofAvailableProvided()
                ? input.vaccineProofAvailable() : pet.isVaccineProofAvailable();

        // 견종을 바꾸면 있는 코드인지 확인함
        // 안 바꿨으면 응답을 만들 때 쓸 값이라 어차피 읽음
        Breed breed = breedRepository.findByCode(breedCode)
                .orElseThrow(() -> {
                    log.warn("없는 견종 코드입니다: accountId={}, breedCode={}", accountId, breedCode);
                    return new CustomException(CommonErrorCode.VALIDATION_FAILED);
                });

        String oldPhotoKey = pet.getPhotoUrl();
        String photoKey = input.photoUrlProvided()
                ? toStoredKey(accountId, input.photoUrl())
                : oldPhotoKey;
        String note = input.noteProvided() ? input.note() : pet.getNote();

        boolean verdictRelevantChanged = pet.isVerdictRelevantChangedFrom(
                weightKg, breedSize, breedCode,
                carrier, stroller, vaccineCompleted, vaccineProofAvailable);
        boolean anythingChanged = verdictRelevantChanged
                || !name.equals(pet.getName())
                || !Objects.equals(photoKey, oldPhotoKey)
                || !Objects.equals(note, pet.getNote());

        pet.update(name, breedCode, weightKg, breedSize,
                carrier, stroller, vaccineCompleted, vaccineProofAvailable, photoKey, note);

        if (anythingChanged) {
            outboxEventRecorder.record(
                    new PetProfileUpdatedEvent(pet.getId(), accountId, verdictRelevantChanged));
            log.info("반려동물을 고쳤습니다: petId={}, verdictRelevantChanged={}",
                    pet.getId(), verdictRelevantChanged);
        }

        // 사진이 실제로 바뀌었을 때만 옛 객체를 지움
        if (oldPhotoKey != null && !oldPhotoKey.equals(photoKey)) {
            afterCommitExecutor.run(() -> storageProvider.delete(oldPhotoKey),
                    "반려동물 옛 사진 삭제 petId=" + pet.getId());
        }

        // save 를 부르지 않음
        // 트랜잭션 안에서 조회한 엔티티라 변경 감지가 커밋 시점에 UPDATE 를 냄
        return toOutput(pet, breed);
    }

    /**
     * 반려동물을 지웁니다.
     *
     * 하드 딜리트입니다. 마지막 한 마리도 지울 수 있습니다.
     * 반려동물 0마리는 정식 상태이며, 한 마리를 키우다 그 아이를 떠나보냈을 때
     * 지울 수 없으면 프로필에 계속 남아 사용자가 서비스를 못 씁니다.
     *
     * 지울 때도 pet.profile.updated 를 참으로 발행합니다.
     * 받는 쪽이 하는 일이 "그 반려동물 캐시를 지운다" 하나라 수정과 같습니다.
     *
     * 대표 반려동물 정리는 프론트가 합니다.
     * 지운 것이 대표였으면 PATCH /users/me/default-pet 을 null 로 한 번 더 부릅니다.
     * 빠뜨려도 user 가 그 식별자로 조회했을 때 0마리로 처리하므로 안전한 쪽으로 실패합니다.
     */
    @Transactional
    public void delete(UUID accountId, UUID petId) {
        Pet pet = getOwnedOrThrow(accountId, petId);

        String photoKey = pet.getPhotoUrl();
        petRepository.delete(pet);

        outboxEventRecorder.record(new PetProfileUpdatedEvent(petId, accountId, true));

        if (photoKey != null) {
            afterCommitExecutor.run(() -> storageProvider.delete(photoKey),
                    "반려동물 사진 삭제 petId=" + petId);
        }

        log.info("반려동물을 지웠습니다: accountId={}, petId={}", accountId, petId);
    }

    /**
     * 내 반려동물을 가져오고, 없거나 남의 것이면 막습니다.
     *
     * 두 경우에 같은 응답을 냅니다.
     * 403 을 주면 "그 반려동물은 있는데 네 것이 아니다" 를 알려 주는 셈이 됩니다.
     */
    private Pet getOwnedOrThrow(UUID accountId, UUID petId) {
        return petRepository.findById(petId)
                .filter(found -> found.getAccountId().equals(accountId))
                .orElseThrow(() -> {
                    log.warn("반려동물을 찾지 못했습니다: accountId={}, petId={}", accountId, petId);
                    return new CustomException(PetErrorCode.PET_NOT_FOUND);
                });
    }

    private Map<String, Breed> loadBreeds(List<Pet> pets) {
        Set<String> codes = pets.stream()
                .map(Pet::getBreedCode)
                .collect(Collectors.toSet());

        return breedRepository.findAllByCodeIn(codes).stream()
                .collect(Collectors.toMap(Breed::getCode, Function.identity()));
    }

    /**
     * 들고 온 사진 주소에서 저장할 키를 꺼냅니다.
     *
     * 우리 버킷의 그 계정 자리가 아니면 거절합니다.
     * 이 검증이 없으면 아래 셋이 그대로 들어옵니다.
     *   프론트가 실수로 보낸 uploadUrl — 서명이 붙어 있어 조회 때 깨짐
     *   남의 사진 주소 — 남의 사진이 내 반려동물에 뜸
     *   아무 문자열 — 조회 때 깨짐
     */
    private String toStoredKey(UUID accountId, String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        return storageProvider.extractOwnedKey(url, accountId)
                .orElseThrow(() -> {
                    log.warn("사진 주소가 발급한 것과 다릅니다: accountId={}", accountId);
                    return new CustomException(CommonErrorCode.VALIDATION_FAILED);
                });
    }

    /**
     * 저장된 값과 견종을 합쳐 응답을 만듭니다.
     *
     * 사진은 볼 때마다 서명해 내보냅니다.
     * 버킷이 퍼블릭 액세스를 막아 두어 저장된 주소 그대로는 열리지 않습니다.
     *
     * 견종을 못 찾는 경우는 등록할 때 막았으므로 생길 수 없습니다.
     * 그래도 목록 전체가 실패하지 않도록 그 셋만 비워 둡니다.
     */
    private PetOutput toOutput(Pet pet, Breed breed) {
        return new PetOutput(
                pet.getId(),
                pet.getName(),
                pet.getWeightKg(),
                pet.getBreedSize(),
                pet.isCarrier(),
                pet.isStroller(),
                pet.isVaccineCompleted(),
                pet.isVaccineProofAvailable(),
                pet.getPhotoUrl() == null ? null : storageProvider.presignDownload(pet.getPhotoUrl()),
                pet.getNote(),
                pet.getBreedCode(),
                breed == null ? null : breed.getNameKo(),
                breed == null ? null : breed.getSpecies(),
                breed != null && breed.isDangerous());
    }
}
