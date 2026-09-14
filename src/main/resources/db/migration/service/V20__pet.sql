-- 이 서비스의 첫 마이그레이션 스크립트입니다.
-- V1 부터 V19 는 공통 모듈이 사용하는 대역이므로 쓰지 않습니다.
--
-- 이미 적용된 스크립트는 수정하지 않습니다.
-- 내용이 바뀌면 체크섬이 달라져 다음 기동이 실패합니다.
-- 변경이 필요하면 다음 번호로 새 스크립트를 만듭니다.
--
-- pet_db 에는 이 두 테이블과 공통 대역의 outbox · processed_event 가 있습니다.
-- 그 둘은 공통 모듈 jar 의 V1__outbox.sql · V2__inbox.sql 이 만들므로
-- 여기서 다시 만들면 "이미 있는 테이블" 로 기동이 실패합니다.
-- pet 은 pet.profile.updated 를 발행하고 account.withdrawn 을 받으므로 둘 다 씁니다.
--
-- 날짜와 시각 컬럼을 전부 timestamp 로 통일해 date 와 time 을 쓰지 않습니다.
-- 국내 전용 서비스이므로 시간대 없는 timestamp 를 쓰고 엔티티는 LocalDateTime 으로 받습니다.
-- 모든 컨테이너에 TZ=Asia/Seoul 이 설정돼 있어야 합니다.


-- =============================================================================
-- breed
-- =============================================================================
-- 견종 마스터. 드롭다운 표기와 맹견 판정, 종 구분에만 씁니다.
--
-- 크기 기본값 컬럼을 두지 않습니다.
-- 크기를 체중으로만 가르기로 해 읽는 곳이 없어졌습니다.
-- 그래서 견종을 늘리거나 줄여도 판정 결과가 달라지지 않습니다.
--
-- 45행짜리 고정 마스터이며 값은 V21 이 채웁니다.
-- 애플리케이션은 읽기만 하고 이 표에 쓰지 않습니다.

CREATE TABLE breed
(
    -- PK 입니다. 대리 키를 두지 않습니다.
    -- 사람이 정한 고정 코드이고 화면과 API 가 이 문자열을 그대로 주고받습니다.
    code         varchar(30)  PRIMARY KEY,

    -- 드롭다운에 보이는 이름입니다.
    name_ko      varchar(40)  NOT NULL,

    -- 동물보호법 시행규칙 제2조의 맹견 5종만 참입니다.
    -- 법은 "그 잡종의 개" 까지 포함하나 믹스는 판정할 수 없으므로
    -- MIX 와 OTHER 는 거짓으로 두고 화면 안내로 보완합니다.
    is_dangerous boolean      NOT NULL,

    -- DOG · CAT · ETC 입니다.
    -- 사용자가 고르지 않고 견종에서 유도합니다.
    -- 값이 늘 수 있으므로 CHECK 를 걸지 않습니다.
    species      varchar(12)  NOT NULL,

    -- 아래 6개 컬럼은 공통 모듈의 BaseEntity 와 짝을 이룹니다.
    -- 빠뜨리면 ddl-auto: validate 가 기동을 막습니다.
    created_at   timestamp    NOT NULL,
    created_by   varchar(45)  NOT NULL,
    updated_at   timestamp    NOT NULL,
    updated_by   varchar(45)  NOT NULL,
    deleted_at   timestamp,
    deleted_by   varchar(45)
);

COMMENT ON TABLE breed IS '견종 마스터. 맹견 판정과 종 구분에만 씁니다.';


-- =============================================================================
-- pet
-- =============================================================================
-- 반려동물. 이 표의 컬럼이 곧 판정 규칙의 입력입니다.
--
-- species 와 is_dangerous_breed 를 두지 않습니다.
-- 둘 다 breed_code 하나에서 정해지는 값이라 복사해 두면 원본과 어긋납니다.
-- 특히 맹견 목록은 시행규칙이 개정되면 바뀌는데, 복사해 두면 이미 등록된 행이
-- 옛값 그대로 남아 판정이 조용히 틀립니다.
--
-- has_leash 를 두지 않습니다.
-- 목줄은 개를 데리고 나가는 사람이면 당연히 있어 판정 축이 되지 못합니다.
-- 목줄이 필요한 장소는 판정이 아니라 준비물 안내로 알립니다.

CREATE TABLE pet
(
    -- PK 는 모든 테이블이 uuid 입니다.
    -- 애플리케이션이 Hibernate 의 @UuidGenerator(VERSION_7) 로 만들어 넣으므로
    -- 여기에 기본값을 지정하지 않습니다.
    id                      uuid          PRIMARY KEY,

    -- 보호자입니다.
    -- auth_db 의 값이라 외래 키를 걸지 않습니다.
    -- 내어 주는 두 internal API 가 이 값과 X-User-Id 를 대조해 소유권을 검증합니다.
    account_id              uuid          NOT NULL,

    name                    varchar(30)   NOT NULL,

    -- breed.code 를 가리킵니다.
    -- 같은 DB 안이지만 외래 키를 걸지 않습니다.
    -- 이 저장소들은 참조 무결성을 애플리케이션에서 지켜 왔고,
    -- 등록할 때 species 와 is_dangerous 를 채우려고 breed 를 어차피 읽으므로
    -- 존재 확인에 드는 비용이 없습니다. 없는 코드는 서비스가 400 으로 막습니다.
    breed_code              varchar(30)   NOT NULL,

    -- SMALL · MEDIUM · LARGE 입니다.
    -- 서버가 체중에서 계산해 채우되 사용자가 고칠 수 있습니다.
    -- 유도값인데도 저장하는 유일한 자리인데, 사용자가 바꿀 수 있어
    -- 계산만으로는 "서버가 채운 값" 과 "사용자가 고친 값" 을 구분할 수 없기 때문입니다.
    -- 값이 늘 수 있으므로 CHECK 를 걸지 않습니다.
    breed_size              varchar(12)   NOT NULL,

    -- 크기를 체중에서만 계산하므로 비어 있으면 breed_size 를 채울 재료가 없습니다.
    -- 그래서 필수입니다.
    --
    -- 0 이하를 막습니다.
    -- 다른 컬럼에 CHECK 를 걸지 않은 이유는 enum 값이 늘면 마이그레이션이 필요해지기
    -- 때문인데, "체중은 양수" 라는 조건은 늘어나거나 바뀔 일이 없습니다.
    -- 이 값이 크기 계산과 판정의 입력이라 0 이나 음수가 들어가면 SMALL 로 떨어져
    -- "동반 가능" 이 잘못 나갑니다.
    --
    -- 상한은 두지 않습니다.
    -- 근거를 댈 수 있는 숫자가 없고, 지나치게 큰 값은 LARGE 로 떨어져
    -- 거절 방향으로 안전하게 틀립니다.
    -- 사람이 보기에 이상한 값은 요청 검증이 400 으로 막습니다.
    weight_kg               numeric(4, 1) NOT NULL
        CONSTRAINT ck_pet_weight_positive CHECK (weight_kg > 0),

    -- 목줄로는 안 되고 이동장이 있어야만 들어갈 수 있는 장소가 실재합니다.
    has_carrier             boolean       NOT NULL,

    -- 이동장과 유모차 중 하나만 있어도 되는 장소가 있어 따로 받습니다.
    has_stroller            boolean       NOT NULL,

    -- 접종은 했는데 증명서가 없어 못 들어가는 곳이 있어 두 값을 따로 받습니다.
    -- 하나만 받으면 vaccine_proof 조건이 항상 판정 불가로 떨어집니다.
    vaccine_completed       boolean       NOT NULL,

    -- 증명서 파일은 받지 않고 보유 여부만 담습니다.
    -- 파일을 받으면 개인정보라 보관과 삭제 책임이 붙습니다.
    vaccine_proof_available boolean       NOT NULL,

    -- S3 주소입니다. 키 설계에 따라 길이가 달라지므로 폭을 못 박지 않습니다.
    photo_url               text,

    note                    varchar(200),

    created_at              timestamp     NOT NULL,
    created_by              varchar(45)   NOT NULL,
    updated_at              timestamp     NOT NULL,
    updated_by              varchar(45)   NOT NULL,
    deleted_at              timestamp,
    deleted_by              varchar(45)
);

-- 내 반려동물 목록을 부르는 자리가 이 컬럼 하나로 좁혀집니다.
-- 그 밖의 조회는 전부 PK 로 들어옵니다.
CREATE INDEX idx_pet_account
    ON pet (account_id);

COMMENT ON TABLE pet IS '반려동물. 이 표의 컬럼이 곧 판정 규칙의 입력입니다.';
