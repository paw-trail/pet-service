package com.pawtrail.pet.application.dto.output;

/**
 * 업로드 주소 발급 결과입니다.
 *
 * 두 주소를 함께 돌려줍니다. 쓰임이 다릅니다.
 *
 *   uploadUrl   브라우저가 파일을 PUT 하는 곳입니다.
 *               서명이 쿼리에 통째로 들어 있고 정해진 시간 뒤 만료됩니다.
 *
 *   fileUrl     *POST /api/v1/pets 에 담아 보낼 값입니다.
 *               서명이 없는 순수한 주소이고, 서버는 이 값에서 키만 꺼내 저장합니다.
 *
 * 둘을 헷갈리면 uploadUrl 이 저장될 수 있는데,
 * 서버가 우리 버킷의 내 자리인지 대조하므로 그 경우 400 이 납니다.
 *
 * @param uploadUrl  브라우저가 직접 PUT 할 주소입니다.
 * @param fileUrl    등록 요청에 담을 최종 주소입니다.
 * @param expiresIn  uploadUrl 이 몇 초 뒤 만료되는지입니다.
 */
public record UploadUrlOutput(String uploadUrl, String fileUrl, long expiresIn) {
}
