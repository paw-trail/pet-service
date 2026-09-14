package com.pawtrail.pet.infrastructure.message.kafka.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * account.withdrawn 의 payload 를 받는 소비 전용 DTO 입니다.
 *
 * auth 의 AccountWithdrawnEvent 와 짝이지만 그 클래스를 공유하지 않습니다.
 * 발행자의 도메인 클래스를 공통 모듈에 올리면 auth 가 필드를 하나 고칠 때마다
 * 받는 쪽 전부가 함께 배포되어, 이벤트로 떼어 놓은 것이 다시 붙습니다.
 *
 * 담긴 값이 식별자 하나뿐입니다.
 * 받는 쪽이 하는 일이 "이 계정의 것을 지운다" 하나라 더 필요한 값이 없고,
 * 이메일이나 닉네임을 담으면 지우려는 개인정보가 소비자들의 로그와 토픽에 남습니다.
 *
 * DomainEvent 를 구현하지 않습니다.
 * 그 인터페이스는 토픽과 집합체 정보를 봉투에 담기 위한 것이라 발행자만 필요합니다.
 *
 * @JsonIgnoreProperties(ignoreUnknown = true) 가 필수입니다.
 * auth 가 payload 에 필드를 더해도 이쪽은 모르는 값을 무시하고 지나가야 합니다.
 * 없으면 발행자가 필드를 하나 늘리는 순간 소비가 전부 실패합니다.
 *
 * @param accountId 계정 식별자입니다. 이 서비스에서는 pet.account_id 의 조건이 됩니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountWithdrawnMessage(UUID accountId) {
}
