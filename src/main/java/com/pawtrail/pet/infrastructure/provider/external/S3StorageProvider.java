package com.pawtrail.pet.infrastructure.provider.external;

import com.pawtrail.pet.domain.provider.StorageProvider;
import com.pawtrail.pet.infrastructure.config.StorageProperties;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * 도메인이 선언한 약속을 S3 로 구현합니다.
 *
 * external 아래에 두는 것은 우리가 만들지 않은 바깥 시스템이기 때문입니다.
 * internal 은 같은 프로젝트의 다른 서비스를 부르는 자리입니다.
 *
 * 서명을 만드는 일은 통신이 아닙니다.
 * 액세스 키로 문자열에 서명하는 계산이라 S3 를 부르지 않고, 그래서 빠르고 실패하지 않습니다.
 * 실제 요청은 그 주소를 받은 브라우저가 보냅니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3StorageProvider implements StorageProvider {

    // 한 사람이 여러 마리를 기르므로 계정당 고정 키를 쓸 수 없음
    // 파일 이름은 서버가 만들며 요청의 fileName 을 쓰지 않음
    private static final String PHOTO_KEY_FORMAT = "pets/%s/%s";

    // 그 계정의 자리인지 볼 때 쓰는 접두사임
    private static final String OWNED_PREFIX_FORMAT = "pets/%s/";

    // 가상 호스팅 방식 주소임
    // 버킷 이름이 호스트 앞에 붙는 형태이고 지금 S3 의 기본임
    // 경로 방식(s3.리전.amazonaws.com/버킷/키)은 옛 방식이라 쓰지 않음
    private static final String PUBLIC_URL_FORMAT = "https://%s.s3.%s.amazonaws.com/%s";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    @Override
    public String newPhotoKey(UUID accountId) {
        return PHOTO_KEY_FORMAT.formatted(accountId, UUID.randomUUID());
    }

    @Override
    public String publicUrl(String key) {
        return PUBLIC_URL_FORMAT.formatted(
                storageProperties.bucket(), storageProperties.region(), key);
    }

    /**
     * 들고 온 주소에서 저장할 키를 꺼냅니다.
     *
     * 문자열을 잘라 쓰지 않고 URI 로 파싱합니다.
     * 접두사만 견주면 서명이 붙은 업로드 주소가 그대로 통과합니다.
     *
     *   https://버킷.s3.리전.amazonaws.com/pets/{계정}/{uuid}?X-Amz-Algorithm=...
     *
     * 이 값은 접두사도 맞고 계정 자리도 맞지만, 잘라 내면 키에 쿼리까지 들어갑니다.
     * 그 문자열로 조회 서명을 만들면 존재하지 않는 객체를 가리켜 사진이 열리지 않습니다.
     *
     * user-service 는 키가 계정당 고정이라 발급한 주소와 완전히 같은지만 보면 됐습니다.
     * 여기는 파일 이름이 매번 달라 그 방법을 쓸 수 없으므로 형태를 하나씩 봅니다.
     */
    @Override
    public Optional<String> extractOwnedKey(String url, UUID accountId) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return Optional.empty();
        }

        // 서명이 붙은 주소를 여기서 걸러 냄
        // 발급한 fileUrl 에는 쿼리도 조각도 없음
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            return Optional.empty();
        }

        if (!"https".equals(uri.getScheme())) {
            return Optional.empty();
        }

        String expectedHost = "%s.s3.%s.amazonaws.com"
                .formatted(storageProperties.bucket(), storageProperties.region());
        if (!expectedHost.equals(uri.getHost())) {
            return Optional.empty();
        }

        // getPath 는 퍼센트 인코딩을 이미 푼 값임
        // 인코딩된 채로 견주면 %2E%2E 같은 표기가 검사를 지나감
        String path = uri.getPath();
        if (path == null || path.length() < 2) {
            return Optional.empty();
        }
        String key = path.substring(1);

        // 그 계정의 자리인지 봄
        //
        // 이 검사가 없으면 남의 키를 자기 반려동물에 붙일 수 있고,
        // 나중에 그 키로 객체를 지우는 코드가 생기면 남의 파일을 지우는 도구가 됨
        if (!key.startsWith(OWNED_PREFIX_FORMAT.formatted(accountId))) {
            return Optional.empty();
        }

        // 발급한 키는 계정 아래 파일 이름 하나뿐임
        // 경로가 더 깊거나 거슬러 올라가는 표기가 섞였으면 우리가 만든 값이 아님
        String fileName = key.substring(OWNED_PREFIX_FORMAT.formatted(accountId).length());
        if (fileName.isBlank() || fileName.contains("/") || fileName.contains("..")) {
            return Optional.empty();
        }

        return Optional.of(key);
    }

    @Override
    public String presignUpload(String key, String contentType, long contentLength) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(key)
                // 서명에 들어가므로 브라우저는 이 타입으로만 올릴 수 있음
                // S3 가 이 값을 객체에 저장해 두었다가 조회할 때 그대로 돌려줌
                .contentType(contentType)
                // 이 값도 서명에 들어감
                // 다른 크기로 올리면 S3 가 거부하므로 클라이언트 검증에 기대지 않게 됨
                //
                // 범위가 아니라 정확한 값임
                // presigned PUT 에는 범위를 걸 수 없고, 버킷 정책에도 크기 조건 키가 없음
                // 프론트가 file.size 를 그대로 보내야 하며 한 바이트라도 다르면 403 이 남
                .contentLength(contentLength)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(storageProperties.uploadExpiresSeconds()))
                .putObjectRequest(objectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    @Override
    public String presignDownload(String key) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(storageProperties.downloadExpiresSeconds()))
                .getObjectRequest(objectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(key)
                .build());

        log.info("객체를 지웠습니다: key={}", key);
    }
}
