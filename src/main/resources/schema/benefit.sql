-- ============================================================
-- 크루 전용 혜택몰(폐쇄몰) 스키마
--
-- prod 프로파일은 spring.jpa.hibernate.ddl-auto: update 이므로
-- 배포하면 Hibernate가 아래 테이블/컬럼을 자동 생성한다.
-- Railway MySQL 콘솔에서 직접 만들고 싶을 때만 이 스크립트를 실행하면 된다.
-- ============================================================


-- ------------------------------------------------------------
-- 1) partners : 기존 테이블에 혜택몰용 컬럼 추가
--    (홈 화면 파트너 배너용 name / image_url / link_url / sort_order 는 그대로 유지)
--    MySQL은 DEFAULT 가 있는 컬럼을 추가하면 기존 행도 그 값으로 채워진다.
-- ------------------------------------------------------------
ALTER TABLE partners
    ADD COLUMN category       VARCHAR(20)  NULL COMMENT 'CAFE/FOOD/EDU/TOOL/ETC',
    ADD COLUMN description    VARCHAR(500) NULL,
    ADD COLUMN logo_image_key VARCHAR(500) NULL COMMENT 'R2 오브젝트 키',
    ADD COLUMN website_url    VARCHAR(500) NULL,
    ADD COLUMN status         VARCHAR(20)  NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/HIDDEN';

-- 혹시 status 가 NULL 로 남은 기존 행이 있으면 보정
UPDATE partners SET status = 'ACTIVE' WHERE status IS NULL;


-- ------------------------------------------------------------
-- 2) benefits : 혜택
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS benefits (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    partner_id    BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL,
    description   LONGTEXT     NULL,
    discount_text VARCHAR(100) NULL COMMENT '예: 아메리카노 20% 할인',
    benefit_type  VARCHAR(20)  NOT NULL COMMENT 'CODE/SHOW/LINK',
    code_value    VARCHAR(100) NULL COMMENT 'CODE 타입일 때만',
    link_url      VARCHAR(500) NULL COMMENT 'LINK 타입일 때만',
    usage_guide   VARCHAR(1000) NULL,
    image_key     VARCHAR(500) NULL COMMENT 'R2 오브젝트 키',
    valid_from    DATE         NULL COMMENT 'NULL 이면 상시',
    valid_to      DATE         NULL COMMENT 'NULL 이면 상시',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/HIDDEN',
    sort_order    INT          NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NULL,
    updated_at    DATETIME(6)  NULL,

    PRIMARY KEY (id),
    KEY idx_benefits_partner (partner_id),
    KEY idx_benefits_listing (status, sort_order, id),
    CONSTRAINT fk_benefits_partner
        FOREIGN KEY (partner_id) REFERENCES partners(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- ------------------------------------------------------------
-- 3) benefit_claims : 혜택 발급 기록
--    같은 혜택을 여러 번 받을 수 있으므로 유니크 제약 없음
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS benefit_claims (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    benefit_id BIGINT      NOT NULL,
    claimed_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    KEY idx_benefit_claims_user (user_id, claimed_at),
    KEY idx_benefit_claims_benefit (benefit_id),
    CONSTRAINT fk_benefit_claims_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_benefit_claims_benefit
        FOREIGN KEY (benefit_id) REFERENCES benefits(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
