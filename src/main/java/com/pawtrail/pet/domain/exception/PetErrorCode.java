package com.pawtrail.pet.domain.exception;

import com.pawtrail.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 이 서비스의 도메인 에러 코드입니다.
 *
 * 공통 코드는 CommonErrorCode 에 있고 도메인 개념은 여기에 둡니다.
 * 공통에 두면 코드 하나를 더할 때마다 공통 모듈 재배포와 전 서비스 버전업이 필요해집니다.
 *
 * getCode 는 반드시 name 을 그대로 반환합니다.
 * 상수 이름이 곧 응답의 code 값이자 API 계약인데, 규칙을 어겨도 컴파일러가 잡지 못합니다.
 *
 * 공통 코드를 쓸지 여기에 둘지는 메시지가 상황을 맞게 말하는가로 가릅니다.
 * 상태 코드가 같아도 사용자에게 보일 문구가 어긋나면 여기에 둡니다.
 *
 * 그 기준으로 둘만 두었습니다.
 * 견종 코드가 없는 것, 사진 주소가 우리 것이 아닌 것, 올리려는 형식이 이미지가 아닌 것은
 * 전부 "입력값이 잘못됐다" 가 맞는 말이고 프론트가 셋을 가려 안내할 화면도 없습니다.
 */
public enum PetErrorCode implements ErrorCode {

    // 그 반려동물이 없거나 내 것이 아님
    //
    // * 두 경우에 같은 코드를 씀
    //   403 을 주면 "그 반려동물은 있는데 네 것이 아니다" 를 알려 주는 셈이라
    //   남의 데이터가 있는지 확인하는 도구가 됨
    //   user 의 VISIT_NOT_FOUND 도 같은 이유로 둘을 묶었음
    //
    // * CommonErrorCode.RESOURCE_NOT_FOUND 를 쓰지 않는 이유
    //   그 메시지가 "요청하신 경로를 찾을 수 없습니다" 임
    //   경로는 맞는데 그 반려동물이 없는 상황이라 사용자에게 주소가 틀린 것처럼 읽힘
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "반려동물을 찾을 수 없습니다."),

    // 관리자가 멈춘 이벤트를 다시 보내려 했으나 그것도 실패함
    //
    // * CommonErrorCode.INTERNAL_SERVER_ERROR 를 쓰지 않는 이유
    //   그 문구로는 서버가 터진 것인지 발행만 실패한 것인지 관리자가 구분할 수 없음
    //   이 값은 관리자 화면에 그대로 뜨는 문구라 상황을 맞게 말해야 함
    //
    // * auth 도 같은 이름을 자기 코드에 두었음. 접두사를 붙이지 않음
    //   경로가 서비스마다 달라 code 값이 겹쳐도 헷갈릴 자리가 없음
    OUTBOX_REPUBLISH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "이벤트를 다시 보내지 못했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    PetErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return this.httpStatus;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
