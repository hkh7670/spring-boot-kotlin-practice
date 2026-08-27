CREATE TABLE admins
(
    id                 BIGINT AUTO_INCREMENT COMMENT '관리자 고유 식별자'
        PRIMARY KEY,
    name               VARCHAR(100)                NOT NULL COMMENT '이름 (AES 암호화 저장)',
    email              VARCHAR(100)                NOT NULL COMMENT '이메일 (AES 암호화 저장)',
    password           VARCHAR(100)                NOT NULL COMMENT '비밀번호 (BCrypt 해시)',
    status             VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL COMMENT '계정 상태 (ACTIVE/INACTIVE/WITHDRAWN)',
    withdrawn_datetime DATETIME(6)                  NULL COMMENT '탈퇴 일시 (개인정보 파기 배치 기준일)',
    created_datetime   DATETIME(6)                  NOT NULL,
    updated_datetime   DATETIME(6)                  NOT NULL,
    CONSTRAINT uq_admins_01
        UNIQUE (email)
)
    COMMENT '관리자 정보';

CREATE TABLE cart_items
(
    id                BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    member_id         BIGINT      NOT NULL COMMENT '장바구니 소유 회원 ID (members.id)',
    product_option_id BIGINT      NOT NULL COMMENT '상품 옵션 ID (product_options.id)',
    count             INT         NOT NULL COMMENT '담은 수량',
    created_datetime  DATETIME(6) NOT NULL,
    updated_datetime  DATETIME(6) NOT NULL,
    CONSTRAINT uq_cart_items_01
        UNIQUE (member_id, product_option_id)
)
    COMMENT '회원별 장바구니 상품';

CREATE INDEX idx_cart_items_01
    ON cart_items (member_id);

CREATE TABLE categories
(
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    parent_id        BIGINT      NULL COMMENT '상위 카테고리 ID (categories.id, 대분류는 NULL)',
    name             VARCHAR(50) NOT NULL COMMENT '카테고리 명',
    level            VARCHAR(20) NOT NULL COMMENT '카테고리 레벨 (LARGE/MEDIUM/SMALL)',
    created_datetime DATETIME(6) NOT NULL,
    updated_datetime DATETIME(6) NOT NULL
)
    COMMENT '상품 카테고리 (대/중/소분류 계층 구조)';

CREATE INDEX idx_categories_01
    ON categories (parent_id);

CREATE TABLE delivery_options
(
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    name             VARCHAR(50) NOT NULL COMMENT '배송 옵션 명',
    price            INT         NOT NULL COMMENT '배송 가격',
    created_datetime DATETIME(6) NOT NULL,
    updated_datetime DATETIME(6) NOT NULL
)
    COMMENT '배송 옵션 관련 정보';

CREATE TABLE members
(
    id               BIGINT AUTO_INCREMENT COMMENT '회원 고유 식별자'
        PRIMARY KEY,
    uuid             VARCHAR(36)  NOT NULL COMMENT '외부 노출용 UUID',
    provider_id      VARCHAR(100) NULL COMMENT 'OAuth 제공자에서 발급한 사용자 ID',
    join_provider    VARCHAR(20)  NOT NULL COMMENT '가입 경로 (일반/OAuth 제공자 구분)',
    email            VARCHAR(100) NULL COMMENT '이메일 (AES 암호화 저장)',
    password         VARCHAR(100) NULL COMMENT '비밀번호 (BCrypt 해시, EMAIL 가입 회원만 보유)',
    last_name        VARCHAR(100) NOT NULL COMMENT '성 (AES 암호화 저장)',
    first_name       VARCHAR(100) NOT NULL COMMENT '이름 (AES 암호화 저장)',
    birth_date       DATE         NOT NULL COMMENT '생년월일',
    phone_number     VARCHAR(100) NOT NULL COMMENT '전화번호 (AES 암호화 저장)',
    role             VARCHAR(20)  NOT NULL COMMENT '회원 권한 (USER, ADMIN 등)',
    created_datetime DATETIME(6)  NOT NULL COMMENT '생성 일시',
    updated_datetime DATETIME(6)  NOT NULL COMMENT '수정 일시',
    CONSTRAINT uq_members_01
        UNIQUE (uuid),
    CONSTRAINT uq_members_02
        UNIQUE (provider_id, join_provider),
    CONSTRAINT uq_members_03
        UNIQUE (email, join_provider)
)
    COMMENT '회원 정보';


CREATE TABLE order_items
(
    id                BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    order_id          BIGINT        NOT NULL COMMENT '주문 ID (orders.id)',
    product_option_id BIGINT        NOT NULL COMMENT '상품 옵션 ID (product_options.id)',
    price             BIGINT        NOT NULL COMMENT '주문 시점의 상품 가격',
    count             INT DEFAULT 1 NOT NULL COMMENT '주문 수량',
    created_datetime  DATETIME(6)   NOT NULL,
    updated_datetime  DATETIME(6)   NOT NULL
)
    COMMENT '주문 상품 정보';

CREATE INDEX idx_order_items_01
    ON order_items (order_id);

CREATE TABLE order_status_histories
(
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    order_id         BIGINT      NOT NULL COMMENT '주문 ID (orders.id)',
    status           VARCHAR(30) NOT NULL COMMENT '변경된 주문 상태 (PENDING_PAYMENT/PAID/CANCELLED)',
    created_datetime DATETIME(6) NOT NULL,
    updated_datetime DATETIME(6) NOT NULL
)
    COMMENT '주문 상태 변경 이력';

CREATE INDEX order_status_histories_01
    ON order_status_histories (order_id);



CREATE TABLE orders
(
    id                  BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    order_uid           VARCHAR(26)                           NOT NULL COMMENT '외부 노출용 주문 식별자 (ULID, Toss orderId)',
    member_id           BIGINT                                NOT NULL COMMENT '주문한 유저의 ID (member.id)',
    product_total_price INT         DEFAULT 0                 NOT NULL COMMENT '상품 전체 가격',
    delivery_option_id  BIGINT                                NOT NULL COMMENT '배송 옵션 정보 ID (delivery_options.id)',
    delivery_price      INT         DEFAULT 0                 NOT NULL COMMENT '주문 시점의 배송 가격 (delivery_info.price 는 이후 변경될 수 있어 스냅샷 저장)',
    status              VARCHAR(30) DEFAULT 'PENDING_PAYMENT' NOT NULL COMMENT '주문 상태 (PENDING_PAYMENT/PAID/CANCELLED)',
    created_datetime    DATETIME(6)                           NOT NULL,
    updated_datetime    DATETIME(6)                           NOT NULL,
    CONSTRAINT uq_orders_01
        UNIQUE (order_uid)
)
    COMMENT '주문 정보';



CREATE TABLE payments
(
    id               BIGINT AUTO_INCREMENT COMMENT '결제 정보 고유 식별자'
        PRIMARY KEY,
    order_id         BIGINT       NOT NULL COMMENT '주문 ID (orders.id)',
    payment_key      VARCHAR(200) NOT NULL COMMENT 'Toss Payments 결제 고유 키',
    amount           INT          NOT NULL COMMENT '결제 금액',
    status           VARCHAR(20)  NOT NULL COMMENT '결제 상태 (READY/IN_PROGRESS/DONE/CANCELED/PARTIAL_CANCELED/ABORTED/EXPIRED)',
    method           VARCHAR(30)  NULL COMMENT '결제 수단 (카드, 가상계좌 등)',
    approved_at      DATETIME(6)  NULL COMMENT '결제 승인 일시',
    created_datetime DATETIME(6)  NOT NULL,
    updated_datetime DATETIME(6)  NOT NULL,
    CONSTRAINT uq_payments_01
        UNIQUE (order_id),
    CONSTRAINT uq_payments_02
        UNIQUE (payment_key)
)
    COMMENT '결제 정보';

CREATE INDEX idx_payments_01
    ON payments (order_id);

CREATE TABLE product_options
(
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    product_id       BIGINT        NOT NULL COMMENT '상품 ID (products.id)',
    name             VARCHAR(100)  NOT NULL COMMENT '옵션 명 (예: 블랙 / L사이즈)',
    price            INT           NOT NULL COMMENT '옵션별 가격',
    stock_count      INT DEFAULT 0 NOT NULL COMMENT '옵션별 재고 수량',
    created_datetime DATETIME(6)   NOT NULL,
    updated_datetime DATETIME(6)   NOT NULL,
    CONSTRAINT uq_product_options_01
        UNIQUE (product_id, name)
)
    COMMENT '상품 옵션(변형) 정보';

CREATE INDEX idx_product_options_01
    ON product_options (product_id);

CREATE TABLE products
(
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    name             VARCHAR(50)   NOT NULL COMMENT '상품 명',
    description      TEXT          NULL COMMENT '상품 상세 설명',
    image_url        VARCHAR(500)  NULL COMMENT '대표 이미지 URL',
    category_id      BIGINT        NULL COMMENT '카테고리 ID (categories.id, 소분류)',
    vendor_id        BIGINT        NULL COMMENT '업체 ID (vendors.id)',
    created_datetime DATETIME(6)   NOT NULL,
    updated_datetime DATETIME(6)   NOT NULL
)
    COMMENT '상품 정보';

CREATE INDEX idx_products_01
    ON products (category_id);

CREATE INDEX idx_products_02
    ON products (vendor_id);

CREATE TABLE vendors
(
    id                            BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    name                          VARCHAR(100) NOT NULL COMMENT '업체명',
    business_registration_number VARCHAR(20)   NULL COMMENT '사업자등록번호',
    contact_number                VARCHAR(20)   NULL COMMENT '업체 연락처',
    created_datetime              DATETIME(6)   NOT NULL,
    updated_datetime              DATETIME(6)   NOT NULL
)
    COMMENT '상품 업체(공급사) 정보';

