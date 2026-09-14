package com.pawtrail.pet.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 객체 저장소 설정입니다.
 *
 * 액세스 키는 여기 없습니다.
 * DefaultCredentialsProvider 가 환경변수에서 읽으며 그 값은 각자 실행 구성에 둡니다.
 * 설정 파일에 두면 config 저장소에 올라가 팀원 전부가 보게 되고 깃 이력에도 남습니다.
 *
 * 값에 검증을 붙였습니다.
 * 빠지면 기동이 실패해 누락이 바로 드러납니다.
 * 없는 채로 뜨면 첫 업로드 요청에서야 알게 되는데 그때는 원인을 찾기가 훨씬 어렵습니다.
 *
 * 검증을 추가할 때는 세 곳을 함께 봐야 합니다.
 *   config 저장소의 pet-service.yml
 *   src/test/resources/application.yml
 *   이 클래스
 * 테스트 yml 은 설정 서버를 끄므로 값이 하나도 안 내려오는데,
 * 여기에 검증이 있으면 contextLoads 가 그 자리에서 깨집니다.
 *
 * @param bucket                  버킷 이름입니다. user-service 와 같은 것을 씁니다.
 * @param region                  리전입니다. 주소에 그대로 들어갑니다.
 * @param uploadExpiresSeconds    업로드 서명 유효 시간입니다.
 * @param downloadExpiresSeconds  조회 서명 유효 시간입니다.
 * @param maxImageBytes           올릴 수 있는 이미지의 최대 크기입니다.
 */
@Validated
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(

        @NotBlank(message = "app.storage.bucket 이 필요합니다")
        String bucket,

        @NotBlank(message = "app.storage.region 이 필요합니다")
        String region,

        @Positive(message = "app.storage.upload-expires-seconds 는 양수여야 합니다")
        long uploadExpiresSeconds,

        @Positive(message = "app.storage.download-expires-seconds 는 양수여야 합니다")
        long downloadExpiresSeconds,

        @Positive(message = "app.storage.max-image-bytes 는 양수여야 합니다")
        long maxImageBytes
) {
}
