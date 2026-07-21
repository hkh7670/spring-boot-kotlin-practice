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
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    order_id         BIGINT        NOT NULL COMMENT '주문 ID (orders.id)',
    product_id       BIGINT        NOT NULL COMMENT '상품 ID (products.id)',
    price            BIGINT        NOT NULL COMMENT '주문 시점의 상품 가격',
    count            INT DEFAULT 1 NOT NULL COMMENT '주문 수량',
    created_datetime DATETIME(6)   NOT NULL,
    updated_datetime DATETIME(6)   NOT NULL
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

CREATE TABLE products
(
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    name             VARCHAR(50)   NOT NULL COMMENT '상품 명',
    price            INT           NOT NULL COMMENT '가격',
    stock_count      INT DEFAULT 0 NOT NULL COMMENT '재고 수량',
    created_datetime DATETIME(6)   NOT NULL,
    updated_datetime DATETIME(6)   NOT NULL
)
    COMMENT '상품 정보';

