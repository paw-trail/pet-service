package com.pawtrail.pet.presentation.controller;

import com.pawtrail.common.response.CommonApiResponse;
import com.pawtrail.common.security.annotation.CurrentUser;
import com.pawtrail.common.security.principal.CustomUserPrincipal;
import com.pawtrail.pet.application.dto.input.PetCreateInput;
import com.pawtrail.pet.application.dto.output.PetOutput;
import com.pawtrail.pet.application.dto.output.UploadUrlOutput;
import com.pawtrail.pet.application.service.PetService;
import com.pawtrail.pet.presentation.request.PetCreateRequest;
import com.pawtrail.pet.presentation.request.PetUpdateRequest;
import com.pawtrail.pet.presentation.request.UploadUrlRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 반려동물 API 입니다.
 *
 * 누구의 반려동물인지는 요청에서 받지 않습니다.
 * 게이트웨이가 토큰을 검증해 X-User-Id 헤더로 넣어 주고,
 * 공통 모듈의 필터가 그것을 CustomUserPrincipal 로 만들어 둡니다.
 *
 * 경로에 accountId 를 두면 남의 것을 부를 수 있게 되므로 그렇게 하지 않습니다.
 *
 * 조회 응답 둘에 no-store 를 붙입니다.
 * 응답의 photoUrl 이 서명된 주소라 정해진 시간 뒤 만료되기 때문입니다.
 *
 * 다만 이것만으로 끝나지 않습니다.
 * 이 헤더가 막는 것은 브라우저 캐시뿐이고, 화면이 응답을 상태로 들고 있는 것은
 * HTTP 헤더를 보지 않습니다. 그쪽은 프론트와 말로 맞춰야 합니다.
 */
@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    /**
     * 반려동물을 등록합니다.
     *
     * 사진이 있으면 upload-url 로 먼저 올리고 그 fileUrl 을 photoUrl 에 담아 보냅니다.
     *
     * 크기를 안 보내면 서버가 체중에서 계산합니다.
     * 보내면 그 값이 이깁니다. 견종 기본값은 보지 않습니다.
     */
    @PostMapping
    public ResponseEntity<CommonApiResponse<PetOutput>> createPet(
            @CurrentUser CustomUserPrincipal principal,
            @Valid @RequestBody PetCreateRequest request) {

        PetOutput response = petService.create(principal.accountId(), new PetCreateInput(
                request.name(),
                request.breedCode(),
                request.weightKg(),
                request.breedSize(),
                request.hasCarrier(),
                request.hasStroller(),
                request.vaccineCompleted(),
                request.vaccineProofAvailable(),
                request.photoUrl(),
                request.note()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonApiResponse.success(response));
    }

    /**
     * 내 반려동물을 전부 봅니다.
     *
     * 등록한 순서대로 내려갑니다.
     * 대표 반려동물을 앞세우지 않습니다. 대표가 누구인지는 user 가 가진 값입니다.
     *
     * 페이징하지 않습니다. 한 사람이 기르는 수만큼이라 잘라야 할 크기가 아닙니다.
     *
     * 응답에 서명된 사진 주소가 실리므로 캐시를 막습니다.
     */
    @GetMapping
    public ResponseEntity<CommonApiResponse<List<PetOutput>>> getMyPets(
            @CurrentUser CustomUserPrincipal principal) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(CommonApiResponse.success(petService.getMyPets(principal.accountId())));
    }

    /**
     * 반려동물 하나를 봅니다.
     *
     * 없는 경우와 남의 것인 경우가 같은 404 입니다.
     *
     * 응답에 서명된 사진 주소가 실리므로 캐시를 막습니다.
     */
    @GetMapping("/{petId}")
    public ResponseEntity<CommonApiResponse<PetOutput>> getPet(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable UUID petId) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(CommonApiResponse.success(petService.getPet(principal.accountId(), petId)));
    }

    /**
     * 반려동물을 고칩니다.
     *
     * 보낸 것만 바꿉니다. 안 보낸 필드는 건드리지 않습니다.
     * 사진만 바꾸려고 보낸 요청이 이름까지 지우면 안 되기 때문입니다.
     *
     * 명시적 null 로 지울 수 있는 것은 사진과 메모뿐입니다.
     * 나머지 여덟은 반려동물이 있는 한 반드시 값이 있어야 하므로 400 입니다.
     *
     * 체중을 바꾸면서 크기를 안 보내면 크기를 다시 계산합니다.
     */
    @PatchMapping("/{petId}")
    public ResponseEntity<CommonApiResponse<PetOutput>> updatePet(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable UUID petId,
            @Valid @RequestBody PetUpdateRequest request) {

        PetOutput response =
                petService.update(principal.accountId(), petId, request.toInput());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(CommonApiResponse.success(response));
    }

    /**
     * 반려동물을 지웁니다.
     *
     * 되돌릴 수 없습니다. 화면에 확인을 두어야 합니다.
     *
     * 마지막 한 마리도 지울 수 있습니다.
     * 한 마리를 키우다 그 아이를 떠나보냈을 때 지울 수 없으면
     * 프로필에 계속 남아 사용자가 서비스를 못 씁니다.
     *
     * 지운 것이 대표 반려동물이었으면 프론트가
     * PATCH /users/me/default-pet 을 null 로 한 번 더 부릅니다.
     */
    @DeleteMapping("/{petId}")
    public ResponseEntity<CommonApiResponse<Void>> deletePet(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable UUID petId) {

        petService.delete(principal.accountId(), petId);

        return ResponseEntity.ok(CommonApiResponse.success(null));
    }

    /**
     * 사진을 올릴 주소를 발급합니다.
     *
     * 이름이 uploads 가 아니라 upload-url 인 이유는 이 서버가 파일을 받지 않기 때문입니다.
     *
     *   ① 여기서 uploadUrl 과 fileUrl 을 받습니다
     *   ② 브라우저가 uploadUrl 로 S3 에 직접 PUT 합니다
     *   ③ fileUrl 을 등록 요청의 photoUrl 에 담아 보냅니다
     *
     * 이 시점에는 반려동물이 아직 없습니다. 그래서 키에 반려동물 식별자가 들어가지 않습니다.
     */
    @PostMapping("/upload-url")
    public ResponseEntity<CommonApiResponse<UploadUrlOutput>> issueUploadUrl(
            @CurrentUser CustomUserPrincipal principal,
            @Valid @RequestBody UploadUrlRequest request) {

        UploadUrlOutput response = petService.issueUploadUrl(
                principal.accountId(), request.contentType(), request.contentLength());

        return ResponseEntity.ok(CommonApiResponse.success(response));
    }
}
