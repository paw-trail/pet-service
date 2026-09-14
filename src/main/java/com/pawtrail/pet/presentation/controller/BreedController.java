package com.pawtrail.pet.presentation.controller;

import com.pawtrail.common.response.CommonApiResponse;
import com.pawtrail.pet.application.dto.output.BreedOutput;
import com.pawtrail.pet.application.service.BreedService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 견종 API 입니다.
 *
 * 반려동물 등록 화면과 수정 화면의 종 드롭다운이 이 결과로 채워집니다.
 *
 * 인증이 필요한 경로입니다.
 * 게이트웨이의 permit-all 목록은 auth 계열뿐이라 여기에는 토큰이 있어야 하는데,
 * 회원가입 Step 2 에서 부르지만 가입이 자동 로그인이라 그 시점에 이미 쿠키가 있습니다.
 *
 * 사용자를 가리지 않는 응답이라 CustomUserPrincipal 을 받지 않습니다.
 */
@RestController
@RequestMapping("/api/v1/breeds")
@RequiredArgsConstructor
public class BreedController {

    private final BreedService breedService;

    /**
     * 견종 목록을 봅니다.
     *
     * 45행 고정 마스터라 페이징하지 않습니다.
     */
    @GetMapping
    public ResponseEntity<CommonApiResponse<List<BreedOutput>>> getBreeds() {
        return ResponseEntity.ok(CommonApiResponse.success(breedService.getBreeds()));
    }
}
