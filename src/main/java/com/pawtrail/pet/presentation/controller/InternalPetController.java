package com.pawtrail.pet.presentation.controller;

import com.pawtrail.common.exception.CommonErrorCode;
import com.pawtrail.common.exception.CustomException;
import com.pawtrail.common.response.CommonApiResponse;
import com.pawtrail.common.security.annotation.CurrentUser;
import com.pawtrail.common.security.principal.CustomUserPrincipal;
import com.pawtrail.pet.application.dto.output.PetInternalOutput;
import com.pawtrail.pet.application.service.PetService;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 다른 서비스가 부르는 조회입니다.
 *
 * 게이트웨이는 /internal 을 라우팅하지 않습니다.
 * 브라우저에서 localhost:8080/internal/... 로 부를 수 없고 같은 VPC 안에서만 닿습니다.
 * 그래서 이 경로에는 인증 토큰이 실려 오지 않고, 공통 보안 체인도 열어 두었습니다.
 *
 * 다만 인증이 없다는 것이 소유권 검증 면제는 아닙니다.
 *
 * 공통 모듈의 RestClientAuthInterceptor 가 원래 사용자의 X-User-Id 를 그대로 실어 보냅니다.
 * SecurityContext 의 CustomUserPrincipal 에서 꺼내 붙이므로,
 * review 가 우리를 부르면 그 요청을 시작한 사람의 식별자가 옵니다.
 * 받는 쪽에서는 HeaderAuthenticationFilter 가 그 헤더로 주체를 다시 만듭니다.
 *
 * 헤더가 없으면 주체가 만들어지지 않아 아래 principal 이 null 입니다.
 * 그때는 거절합니다.
 * 배치나 스케줄러가 부르면 SecurityContext 가 비어 헤더 없이 나가는데,
 * "없으면 통과" 로 두면 헤더를 안 보내는 것이 곧 우회가 되어 검증을 둔 의미가 사라집니다.
 * 지금 이 API 를 배치가 부를 계획이 없고, 나중에 필요하면 경로를 따로 냅니다.
 *
 * user 의 /internal/users?ids= 는 소유권을 보지 않습니다.
 * 그쪽은 닉네임과 사진이 후기 목록에 그대로 보이는 값이라 숨길 것이 없습니다.
 * 여기는 체중과 접종 여부처럼 화면 어디에도 안 나가는 값을 주므로 다릅니다.
 */
@RestController
@RequestMapping("/internal/pets")
@RequiredArgsConstructor
@Validated
public class InternalPetController {

    private final PetService petService;

    /**
     * 여러 반려동물을 한 번에 돌려줍니다.
     *
     * review 가 후기를 쓸 때 견종과 체중을 스냅샷으로 복사해 가는 경로입니다.
     *
     * 한 번에 100개까지 받습니다. 넘으면 400 입니다.
     * 식별자를 ids=a&ids=b 처럼 주소에 실어 오므로 하나에 41바이트가 들고,
     * Tomcat 이 요청 줄과 헤더를 합쳐 8KB 까지만 받아 180개 언저리가 이미 천장입니다.
     * 그 천장을 넘으면 컨트롤러에 닿기 전에 공통 응답 형태가 아닌 400 이 나가고
     * 이 서비스 로그에도 거의 남지 않습니다. 100 은 그 절반입니다.
     * 천장을 없애는 길은 부르는 쪽이 100개씩 나눠 부르는 것입니다.
     *
     * 상한은 @Size 로 겁니다.
     * 넘으면 스프링 MVC 가 HandlerMethodValidationException 을 던지고
     * common 0.0.14 가 400 으로 돌려줍니다.
     *
     * 부른 사람의 것이 아닌 식별자는 결과에서 빠집니다. 오류가 아닙니다.
     * 없는 식별자도 같습니다. 남의 것을 "없는 것처럼" 다루면 존재 여부도 새지 않습니다.
     */
    @GetMapping
    public ResponseEntity<CommonApiResponse<List<PetInternalOutput>>> getPets(
            @CurrentUser CustomUserPrincipal principal,
            @RequestParam("ids")
            @Size(max = 100, message = "한 번에 100개까지 조회할 수 있습니다.")
            List<UUID> ids) {

        List<PetInternalOutput> response =
                petService.getInternalPets(requireAccountId(principal), ids);

        return ResponseEntity.ok(CommonApiResponse.success(response));
    }

    /**
     * 반려동물 하나를 돌려줍니다.
     *
     * verdict 가 판정 재료로 부르는 경로입니다.
     *
     * 없거나 부른 사람의 것이 아니면 404 입니다.
     * 공개 API 와 같은 규칙입니다.
     */
    @GetMapping("/{petId}")
    public ResponseEntity<CommonApiResponse<PetInternalOutput>> getPet(
            @CurrentUser CustomUserPrincipal principal,
            @PathVariable UUID petId) {

        PetInternalOutput response =
                petService.getInternalPet(requireAccountId(principal), petId);

        return ResponseEntity.ok(CommonApiResponse.success(response));
    }

    /**
     * 부른 사람을 꺼냅니다. 없으면 거절합니다.
     *
     * @CurrentUser 는 @AuthenticationPrincipal 의 별칭이라
     * 인증 주체가 없으면 null 이 들어옵니다.
     */
    private UUID requireAccountId(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new CustomException(CommonErrorCode.AUTHENTICATION_FAILED);
        }
        return principal.accountId();
    }
}
