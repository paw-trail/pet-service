package com.pawtrail.pet.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * 사진 업로드 주소 발급 요청입니다.
 *
 * 이 API 는 주소만 발급하고 브라우저가 S3 로 직접 PUT 합니다.
 * 이름이 uploads 가 아니라 upload-url 인 이유가 그것입니다.
 *
 * user-service 의 같은 API 와 달리 fileName 을 받습니다.
 * 다만 키에 쓰지는 않습니다. 형식을 아는 데만 씁니다.
 * 키에 넣으면 정규화를 한 번 빠뜨렸을 때 남의 계정 자리를 가리키는 구멍이 됩니다.
 *
 * @param fileName     올릴 파일의 이름입니다. 화면이 보여 줄 값이며 키에는 쓰지 않습니다.
 * @param contentType  이미지 형식입니다.
 *                     서명에 들어가므로 브라우저는 이 타입으로만 올릴 수 있고,
 *                     S3 가 그 값을 객체에 저장해 조회할 때 그대로 돌려줍니다.
 *                     image/jpeg 와 image/png 만 받습니다.
 * @param contentLength 파일 크기입니다.
 *                     이 값도 서명에 들어가 다른 크기로 올리면 S3 가 거부합니다.
 *                     범위가 아니라 정확한 값이라 프론트가 file.size 를 그대로 보내야 합니다.
 */
public record UploadUrlRequest(

        @NotBlank(message = "파일 이름은 필수입니다.")
        String fileName,

        @NotBlank(message = "형식은 필수입니다.")
        @Pattern(regexp = "image/(jpeg|png)",
                message = "image/jpeg 또는 image/png 만 올릴 수 있습니다")
        String contentType,

        @NotNull(message = "파일 크기는 필수입니다.")
        @Positive(message = "파일 크기는 0보다 커야 합니다.")
        Long contentLength
) {
}
