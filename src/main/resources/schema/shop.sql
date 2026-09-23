-- ============================================================
-- 크루 전용 쇼핑몰 스키마
--
-- prod 프로파일은 spring.jpa.hibernate.ddl-auto: update 이므로
-- 배포하면 Hibernate가 아래 테이블을 자동 생성한다.
-- Railway MySQL 콘솔에서 직접 만들고 싶을 때만 이 스크립트를 실행한다.
--
-- 주의: 주문 테이블명은 MySQL 예약어(ORDER) 회피를 위해 shop_orders 를 쓴다.
-- ============================================================


-- ------------------------------------------------------------
-- 1) products : 상품
--
-- 상품은 제휴 업체(partners)에 소속된다. 화면에 노출되는 업체명/카테고리는
-- partners 에서 파생되므로 products 에는 category 컬럼이 없다.
-- supplier_name / product_code 는 발주용 내부 정보라 어드민에게만 보인다.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(200) NOT NULL,
    partner_id     BIGINT       NOT NULL COMMENT '제휴 업체 (partners.id)',
    original_price INT          NOT NULL COMMENT '정가',
    crew_price     INT          NOT NULL COMMENT '크루 전용가',
    stock          INT          NOT NULL DEFAULT 0,
    supplier_name  VARCHAR(100) NULL COMMENT '발주용 내부 정보 (노출 X)',
    product_code   VARCHAR(100) NULL COMMENT '발주용 업체 측 상품코드 (노출 X)',
    thumbnail_key  VARCHAR(500) NULL COMMENT 'R2 오브젝트 키',
    description    LONGTEXT     NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE(판매중)/HIDDEN/SOLD_OUT',
    sort_order     INT          NOT NULL DEFAULT 0,
    created_at     DATETIME(6)  NULL,
    updated_at     DATETIME(6)  NULL,

    PRIMARY KEY (id),
    KEY idx_products_listing (status, sort_order, id),
    KEY idx_products_partner (partner_id, status),
    CONSTRAINT fk_products_partner
        FOREIGN KEY (partner_id) REFERENCES partners(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- ------------------------------------------------------------
-- 1-1) 이미 products 를 만들어 둔 개발 DB용 마이그레이션
--
-- 쇼핑몰은 아직 미배포라 운영 데이터가 없다. 로컬/개발 DB에만 적용한다.
-- NOT NULL FK 라서 기존 행이 있으면 먼저 소속 업체를 정해줘야 한다.
-- ------------------------------------------------------------
-- ALTER TABLE products ADD COLUMN partner_id BIGINT NULL COMMENT '제휴 업체 (partners.id)';
-- UPDATE products SET partner_id = (SELECT MIN(id) FROM partners) WHERE partner_id IS NULL;
-- ALTER TABLE products MODIFY COLUMN partner_id BIGINT NOT NULL;
-- ALTER TABLE products ADD CONSTRAINT fk_products_partner
--     FOREIGN KEY (partner_id) REFERENCES partners(id);
-- ALTER TABLE products ADD KEY idx_products_partner (partner_id, status);
-- ALTER TABLE products DROP KEY idx_products_category;
-- ALTER TABLE products DROP COLUMN category;


-- ------------------------------------------------------------
-- 2) product_images : 상세 이미지
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_images (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    product_id BIGINT       NOT NULL,
    image_key  VARCHAR(500) NOT NULL COMMENT 'R2 오브젝트 키',
    sort_order INT          NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    KEY idx_product_images_product (product_id, sort_order),
    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- ------------------------------------------------------------
-- 3) cart_items : 장바구니 (유저 + 상품 유니크)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    product_id BIGINT      NOT NULL,
    quantity   INT         NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_user_product (user_id, product_id),
    KEY idx_cart_items_product (product_id),
    CONSTRAINT fk_cart_items_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- ------------------------------------------------------------
-- 4) shop_orders : 주문
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS shop_orders (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    order_number   VARCHAR(64)  NOT NULL COMMENT '토스 orderId 로도 사용',
    user_id        BIGINT       NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                   COMMENT 'PENDING/PAID/PREPARING/SHIPPING/DELIVERED/CANCELLED',
    total_amount   INT          NOT NULL,
    receiver_name  VARCHAR(50)  NOT NULL,
    receiver_phone VARCHAR(30)  NOT NULL,
    postcode       VARCHAR(10)  NULL,
    address        VARCHAR(300) NOT NULL,
    address_detail VARCHAR(200) NULL,
    delivery_memo  VARCHAR(200) NULL,
    payment_key    VARCHAR(200) NULL COMMENT '토스 결제 키',
    paid_at        DATETIME(6)  NULL,
    cancelled_at   DATETIME(6)  NULL,
    created_at     DATETIME(6)  NULL,
    updated_at     DATETIME(6)  NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_shop_orders_number (order_number),
    KEY idx_shop_orders_user (user_id, id),
    KEY idx_shop_orders_status (status, id),
    CONSTRAINT fk_shop_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- ------------------------------------------------------------
-- 5) shop_order_items : 주문 상품 (상품명·단가는 주문 시점 스냅샷)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS shop_order_items (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    order_id     BIGINT       NOT NULL,
    product_id   BIGINT       NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    unit_price   INT          NOT NULL,
    quantity     INT          NOT NULL,

    PRIMARY KEY (id),
    KEY idx_shop_order_items_order (order_id),
    KEY idx_shop_order_items_product (product_id),
    CONSTRAINT fk_shop_order_items_order   FOREIGN KEY (order_id)   REFERENCES shop_orders(id),
    CONSTRAINT fk_shop_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
