package com.pawtrail.pet.application.dto.output;

/**
 * 견종 드롭다운 항목입니다.
 *
 * 맹견 여부를 담지 않습니다.
 * 화면에 노출할 정보가 아니고, 어느 견종이 맹견인지는 서버가 저장 시점에 판정합니다.
 *
 * 종도 담지 않습니다.
 * 등록하고 나면 GET /pets 응답에 실려 오므로 드롭다운에는 필요가 없습니다.
 *
 * @param code    저장할 때 보낼 값입니다.
 * @param nameKo  화면에 보일 이름입니다.
 */
public record BreedOutput(String code, String nameKo) {
}
