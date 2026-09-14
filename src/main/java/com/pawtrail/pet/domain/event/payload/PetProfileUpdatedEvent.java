package com.pawtrail.pet.domain.event.payload;

import com.pawtrail.common.message.DomainEvent;
import java.util.UUID;

/**
 * 반려동물 프로필이 달라졌음을 알리는 이벤트입니다.
 *
 * verdict 가 받아 그 반려동물의 판정 캐시를 지웁니다.
 *
 * verdictRelevantChanged 하나로 무효화 범위를 가릅니다.
 * 스냅샷 표를 두지 않고 불리언 하나로 끝내려고 이렇게 정했습니다.
 * 이름이나 사진만 바뀌면 거짓이라 받는 쪽이 아무것도 하지 않습니다.
 *
 * 판정 축은 일곱입니다.
 *   체중 · 크기 · 이동장 · 유모차 · 접종 여부 · 증명서 보유 · 견종
 * 견종이 축인 것은 맹견 판정 때문입니다.
 * 견종이 바뀌면 맹견 여부가 달라져 판정이 뒤집힐 수 있습니다.
 *
 * 삭제할 때도 이 이벤트를 참으로 발행합니다.
 * 이름이 "updated" 인데 지워진 경우에도 쓰는 것이 어색하지만,
 * pet.deleted 를 따로 만들면 토픽과 소비자와 Inbox 가 통째로 붙는 데 비해
 * 받는 쪽이 하는 일은 "그 반려동물 캐시를 지운다" 하나로 같습니다.
 * 소비자 코드를 한 줄도 고치지 않아도 되므로 있는 토픽을 그대로 씁니다.
 *
 * 값이 하나도 안 바뀐 수정은 이 이벤트를 발행하지 않습니다.
 * 이벤트의 뜻이 "네가 가진 것이 낡았다" 인데 안 바뀌었으면 낡지 않았습니다.
 *
 * @param petId                  반려동물 식별자입니다.
 * @param accountId              보호자입니다. 받는 쪽이 사용자 단위로 캐시를 잡을 수 있습니다.
 * @param verdictRelevantChanged 판정에 쓰이는 값이 달라졌는지입니다.
 */
public record PetProfileUpdatedEvent(
        UUID petId,
        UUID accountId,
        boolean verdictRelevantChanged
) implements DomainEvent {

    @Override
    public String getTopic() {
        // infra 의 create-topics.sh 에 같은 이름이 있어야 함
        // 토픽 자동 생성을 꺼 두었으므로 없으면 발행이 실패함
        return "pet.profile.updated";
    }

    @Override
    public String getAggregateType() {
        return "Pet";
    }

    @Override
    public String getAggregateId() {
        // 파티션 키가 되어 같은 반려동물에 대한 이벤트의 순서를 보장함
        return petId.toString();
    }
}
