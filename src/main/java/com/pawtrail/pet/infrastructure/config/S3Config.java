package com.pawtrail.pet.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3 클라이언트를 만듭니다.
 *
 * 둘을 만듭니다.
 *   S3Client     서버가 직접 부르는 것. 지금은 삭제에만 씁니다.
 *   S3Presigner  브라우저에게 건네줄 서명된 주소를 만드는 것.
 *                실제 요청은 브라우저가 보내므로 이 객체는 통신을 하지 않습니다.
 *
 * 인증 정보를 코드에 두지 않습니다.
 * DefaultCredentialsProvider 가 정해진 순서로 찾습니다.
 *   자바 시스템 속성 → 환경변수 → 자격 증명 파일 → EC2 인스턴스 역할
 * 로컬에서는 실행 구성의 환경변수를 쓰고,
 * 나중에 EC2 로 옮기면 인스턴스 역할을 붙여 키 자체를 없앨 수 있습니다.
 * 그때 이 코드는 한 줄도 안 바뀝니다.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@RequiredArgsConstructor
public class S3Config {

    private final StorageProperties storageProperties;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(storageProperties.region()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(storageProperties.region()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
