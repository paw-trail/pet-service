package com.pawtrail.pet.infrastructure.provider.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.pawtrail.pet.infrastructure.config.StorageProperties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 들고 온 주소에서 키를 꺼내는 규칙을 검사합니다.
 *
 * S3 를 부르지 않는 순수한 문자열 판단이라 목이 필요하지 않습니다.
 * 클라이언트와 저장소가 만나는 자리이고, 여기서 새면 남의 파일을 가리키는 값이
 * 그대로 저장되므로 경우를 하나씩 못 박아 둡니다.
 *
 * 처음 판에서 접두사만 견주었더니 서명이 붙은 업로드 주소가 그대로 통과했습니다.
 * 그러면 키에 쿼리까지 들어가 조회할 때 존재하지 않는 객체를 가리킵니다.
 */
class S3StorageProviderTest {

    private static final String BUCKET = "pawtrail-media";
    private static final String REGION = "ap-northeast-2";
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID OTHER_ACCOUNT_ID = UUID.randomUUID();

    private S3StorageProvider storageProvider;

    @BeforeEach
    void setUp() {
        // S3Client 와 S3Presigner 는 이 검사에서 쓰이지 않음
        storageProvider = new S3StorageProvider(null, null,
                new StorageProperties(BUCKET, REGION, 600, 3600, 20971520L));
    }

    @Test
    @DisplayName("발급한 주소에서 키를 꺼낸다")
    void 발급한_주소에서_키를_꺼낸다() {
        String key = "pets/%s/%s".formatted(ACCOUNT_ID, UUID.randomUUID());

        assertThat(storageProvider.extractOwnedKey(url(key), ACCOUNT_ID))
                .contains(key);
    }

    @Test
    @DisplayName("서명이 붙은 업로드 주소는 거절한다")
    void 서명이_붙은_업로드_주소는_거절한다() {
        String key = "pets/%s/%s".formatted(ACCOUNT_ID, UUID.randomUUID());
        String signed = url(key) + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=abc";

        assertThat(storageProvider.extractOwnedKey(signed, ACCOUNT_ID)).isEmpty();
    }

    @Test
    @DisplayName("조각이 붙은 주소도 거절한다")
    void 조각이_붙은_주소도_거절한다() {
        String key = "pets/%s/%s".formatted(ACCOUNT_ID, UUID.randomUUID());

        assertThat(storageProvider.extractOwnedKey(url(key) + "#anything", ACCOUNT_ID)).isEmpty();
    }

    @Test
    @DisplayName("남의 계정 자리는 거절한다")
    void 남의_계정_자리는_거절한다() {
        String key = "pets/%s/%s".formatted(OTHER_ACCOUNT_ID, UUID.randomUUID());

        assertThat(storageProvider.extractOwnedKey(url(key), ACCOUNT_ID)).isEmpty();
    }

    @Test
    @DisplayName("다른 버킷이나 다른 호스트는 거절한다")
    void 다른_버킷이나_다른_호스트는_거절한다() {
        String key = "pets/%s/%s".formatted(ACCOUNT_ID, UUID.randomUUID());

        assertThat(storageProvider.extractOwnedKey(
                "https://evil.s3.%s.amazonaws.com/%s".formatted(REGION, key), ACCOUNT_ID))
                .isEmpty();
        assertThat(storageProvider.extractOwnedKey(
                "https://example.com/%s".formatted(key), ACCOUNT_ID))
                .isEmpty();
    }

    @Test
    @DisplayName("https 가 아니면 거절한다")
    void https_가_아니면_거절한다() {
        String key = "pets/%s/%s".formatted(ACCOUNT_ID, UUID.randomUUID());

        assertThat(storageProvider.extractOwnedKey(
                url(key).replace("https://", "http://"), ACCOUNT_ID))
                .isEmpty();
    }

    @Test
    @DisplayName("경로가 더 깊거나 거슬러 올라가면 거절한다")
    void 경로가_더_깊거나_거슬러_올라가면_거절한다() {
        assertThat(storageProvider.extractOwnedKey(
                url("pets/%s/sub/file".formatted(ACCOUNT_ID)), ACCOUNT_ID)).isEmpty();
        assertThat(storageProvider.extractOwnedKey(
                url("pets/%s/../%s/file".formatted(ACCOUNT_ID, OTHER_ACCOUNT_ID)), ACCOUNT_ID))
                .isEmpty();
        assertThat(storageProvider.extractOwnedKey(
                url("pets/%s/".formatted(ACCOUNT_ID)), ACCOUNT_ID)).isEmpty();
    }

    @Test
    @DisplayName("빈 값과 주소가 아닌 값은 거절한다")
    void 빈_값과_주소가_아닌_값은_거절한다() {
        assertThat(storageProvider.extractOwnedKey(null, ACCOUNT_ID)).isEmpty();
        assertThat(storageProvider.extractOwnedKey("  ", ACCOUNT_ID)).isEmpty();
        assertThat(storageProvider.extractOwnedKey("한글 주소", ACCOUNT_ID)).isEmpty();
    }

    private String url(String key) {
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(BUCKET, REGION, key);
    }
}
