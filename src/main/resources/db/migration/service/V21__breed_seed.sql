-- 견종 마스터를 채웁니다.
--
-- breed 표는 애플리케이션이 읽기만 하는 고정 마스터입니다.
-- 견종을 더하거나 고치는 일은 다음 번호의 마이그레이션으로 합니다.
--
-- created_by 와 updated_by 를 'flyway' 로 직접 씁니다.
-- BaseEntity 의 두 컬럼이 NOT NULL 인데 SQL INSERT 라 JPA Auditing 이 돌지 않습니다.
-- 값을 'system' 이 아니라 'flyway' 로 둔 것은 어느 경로로 들어온 행인지 드러나게 하려는 것이며,
-- 배치가 남기는 ingest-batch · extract-batch 와 같은 결입니다.
--
-- 목록의 근거입니다.
--   상위 7종   KB금융지주 경영연구소 「한국 반려동물보고서」
--   맹견 5종   동물보호법 시행규칙 제2조
--   나머지     국내에서 흔한 견종으로 골랐으며 순위를 주장하지 않습니다
--
-- 크기를 체중으로만 가르기로 해 목록을 늘리거나 줄여도 판정 결과가 달라지지 않습니다.
-- 견종이 판정에 쓰이는 곳은 맹견 여부 하나뿐입니다.

INSERT INTO breed (code, name_ko, is_dangerous, species,
                   created_at, created_by, updated_at, updated_by)
VALUES
    -- 일반 견종 38종
    ('MALTESE',                    '말티즈',                      false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('POODLE',                     '푸들',                        false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('POMERANIAN',                 '포메라니안',                   false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('JINDO',                      '진돗개',                      false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('SHIH_TZU',                   '시츄',                        false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('BICHON_FRISE',               '비숑 프리제',                  false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('CHIHUAHUA',                  '치와와',                      false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('YORKSHIRE_TERRIER',          '요크셔테리어',                  false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('DACHSHUND',                  '닥스훈트',                     false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('MINIATURE_PINSCHER',         '미니어처 핀셔',                 false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('SCHNAUZER',                  '슈나우저',                     false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('PAPILLON',                   '파피용',                      false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('PEKINGESE',                  '페키니즈',                     false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('SPITZ',                      '스피츠',                      false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('WESTIE',                     '웨스트 하이랜드 화이트 테리어',    false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('COCKER_SPANIEL',             '코커스패니얼',                  false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('WELSH_CORGI',                '웰시코기',                     false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('BEAGLE',                     '비글',                        false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('SHIBA_INU',                  '시바견',                      false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('BORDER_COLLIE',              '보더콜리',                     false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('FRENCH_BULLDOG',             '프렌치 불도그',                 false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('BULLDOG',                    '불도그',                      false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('PUG',                        '퍼그',                        false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('BOSTON_TERRIER',             '보스턴테리어',                  false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('JACK_RUSSELL_TERRIER',       '잭 러셀 테리어',                false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('SAPSALI',                    '삽살개',                      false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('PUNGSAN',                    '풍산개',                      false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('GOLDEN_RETRIEVER',           '골든 리트리버',                 false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('LABRADOR_RETRIEVER',         '래브라도 리트리버',              false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('SIBERIAN_HUSKY',             '시베리안 허스키',                false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('SAMOYED',                    '사모예드',                     false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('GERMAN_SHEPHERD',            '저먼 셰퍼드',                   false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('GREAT_PYRENEES',             '그레이트 피레니즈',              false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('ALASKAN_MALAMUTE',           '알래스칸 맬러뮤트',              false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('DALMATIAN',                  '달마시안',                     false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('DOBERMAN',                   '도베르만',                     false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('GREYHOUND',                  '그레이하운드',                  false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('POINTER',                    '포인터',                      false, 'DOG', now(), 'flyway', now(), 'flyway'),

    -- 맹견 5종
    -- 동물보호법 시행규칙 제2조입니다.
    -- 법은 "그 잡종의 개" 까지 포함하나 잡종은 판정할 수 없으므로
    -- MIX 는 거짓으로 두고 화면 안내로 보완합니다.
    --
    -- AMERICAN_STAFFORDSHIRE 는 법령 명칭을 그대로 쓰면 코드가 30자가 되어
    -- varchar(30) 에 여유가 없어집니다. 코드만 줄이고 이름은 법령 그대로 둡니다.
    ('TOSA',                       '도사견',                      true,  'DOG', now(), 'flyway', now(), 'flyway'),
    ('AMERICAN_PIT_BULL_TERRIER',  '아메리칸 핏불테리어',            true,  'DOG', now(), 'flyway', now(), 'flyway'),
    ('AMERICAN_STAFFORDSHIRE',     '아메리칸 스태퍼드셔 테리어',      true,  'DOG', now(), 'flyway', now(), 'flyway'),
    ('STAFFORDSHIRE_BULL_TERRIER', '스태퍼드셔 불 테리어',           true,  'DOG', now(), 'flyway', now(), 'flyway'),
    ('ROTTWEILER',                 '로트와일러',                   true,  'DOG', now(), 'flyway', now(), 'flyway'),

    -- 목록 맨 끝 두 줄입니다.
    -- 조회할 때 이름순이 아니라 언제나 마지막으로 내려갑니다.
    --
    -- MIX 의 표기를 "믹스견" 이 아니라 "믹스 · 목록에 없는 견종" 으로 넓혔습니다.
    -- 목록을 작게 가져가기로 해 순종인데 목록에 없는 개가 생기는데,
    -- 그 보호자가 OTHER 를 고르면 species 가 ETC 가 되어
    -- 장소 상세에 "이 기준은 강아지 기준이라 다를 수 있습니다" 가 잘못 뜹니다.
    ('MIX',                        '믹스 · 목록에 없는 견종',        false, 'DOG', now(), 'flyway', now(), 'flyway'),
    ('OTHER',                      '그 외 (고양이 등)',             false, 'ETC', now(), 'flyway', now(), 'flyway');
