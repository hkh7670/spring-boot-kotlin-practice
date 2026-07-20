-- auto-generated definition
CREATE TABLE delivery_info
(
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    name             VARCHAR(50) NOT NULL COMMENT '배송 옵션 명',
    price            INT         NOT NULL COMMENT '배송 가격',
    created_datetime DATETIME(6) NOT NULL,
    updated_datetime DATETIME(6) NOT NULL
) COMMENT '배송 관련 정보';


-- auto-generated definition
CREATE TABLE member
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
    CONSTRAINT member_unique_1
        UNIQUE (uuid),
    CONSTRAINT member_unique_2
        UNIQUE (provider_id, join_provider),
    CONSTRAINT member_unique_3
        UNIQUE (email, join_provider)
) COMMENT '회원 정보';

-- auto-generated definition
CREATE TABLE order_detail_info
(
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    order_info_id    BIGINT        NOT NULL COMMENT '주문 ID (order_info.id)',
    product_info_id  BIGINT        NOT NULL COMMENT '상품 ID (product_info.id)',
    price            BIGINT        NOT NULL COMMENT '주문 시점의 상품 가격',
    count            INT DEFAULT 1 NOT NULL COMMENT '주문 수량',
    created_datetime DATETIME(6)   NOT NULL,
    updated_datetime DATETIME(6)   NOT NULL
) COMMENT '주문 상세 정보';

-- auto-generated definition
CREATE TABLE order_info
(
    id                  BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    order_uid           VARCHAR(26)                           NOT NULL COMMENT '외부 노출용 주문 식별자 (ULID, Toss orderId)',
    member_id           BIGINT                                NOT NULL COMMENT '주문한 유저의 ID (member.id)',
    product_total_price INT         DEFAULT 0                 NOT NULL COMMENT '상품 전체 가격',
    delivery_info_id    BIGINT                                NOT NULL COMMENT '배송 정보 ID (delivery_info.id)',
    delivery_price      INT         DEFAULT 0                 NOT NULL COMMENT '주문 시점의 배송 가격 (delivery_info.price 는 이후 변경될 수 있어 스냅샷 저장)',
    status              VARCHAR(30) DEFAULT 'PENDING_PAYMENT' NOT NULL COMMENT '주문 상태 (PENDING_PAYMENT/PAID/CANCELLED)',
    created_datetime    DATETIME(6)   NOT NULL,
    updated_datetime    DATETIME(6)   NOT NULL,
    CONSTRAINT order_info_unique_1
        UNIQUE (order_uid)
) COMMENT '주문 정보';

-- auto-generated definition
CREATE TABLE order_status_history
(
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    order_info_id    BIGINT      NOT NULL COMMENT '주문 ID (order_info.id)',
    status           VARCHAR(30) NOT NULL COMMENT '변경된 주문 상태 (PENDING_PAYMENT/PAID/CANCELLED)',
    created_datetime DATETIME(6) NOT NULL,
    updated_datetime DATETIME(6) NOT NULL
) COMMENT '주문 상태 변경 이력';

-- auto-generated definition
CREATE TABLE product_info
(
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    name             VARCHAR(50)   NOT NULL COMMENT '상품 명',
    price            INT           NOT NULL COMMENT '가격',
    stock_count      INT DEFAULT 0 NOT NULL COMMENT '재고 수량',
    created_datetime DATETIME(6)   NOT NULL,
    updated_datetime DATETIME(6)   NOT NULL
) COMMENT '상품 정보';


-- auto-generated definition
CREATE TABLE pay_info
(
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY COMMENT '결제 정보 고유 식별자',
    order_info_id    BIGINT                             NOT NULL COMMENT '주문 ID (order_info.id)',
    payment_key      VARCHAR(200)                       NOT NULL COMMENT 'Toss Payments 결제 고유 키',
    amount           INT                                NOT NULL COMMENT '결제 금액',
    status           VARCHAR(20)                        NOT NULL COMMENT '결제 상태 (READY/IN_PROGRESS/DONE/CANCELED/PARTIAL_CANCELED/ABORTED/EXPIRED)',
    method           VARCHAR(30) NULL COMMENT '결제 수단 (카드, 가상계좌 등)',
    approved_at      DATETIME(6)                        NULL COMMENT '결제 승인 일시',
    created_datetime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_datetime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pay_info_unique_1
        UNIQUE (order_info_id),
    CONSTRAINT pay_info_unique_2
        UNIQUE (payment_key),
    CONSTRAINT pay_info_order_info_fk
        FOREIGN KEY (order_info_id) REFERENCES order_info (id)
) COMMENT '결제 정보';

-- 기존 DB에 컬럼만 추가할 때 사용 (mysql/dev 프로파일은 ddl-auto: none 이라 수동 실행 필요)
ALTER TABLE order_info
    ADD COLUMN delivery_price INT DEFAULT 0 NOT NULL COMMENT '주문 시점의 배송 가격 (delivery_info.price 는 이후 변경될 수 있어 스냅샷 저장)'
    AFTER delivery_info_id;

-- 기존 DB에 주문 상태 이력 테이블을 추가할 때 사용 (mysql/dev 프로파일은 ddl-auto: none 이라 수동 실행 필요)
CREATE TABLE order_status_history
(
    id               BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    order_info_id    BIGINT      NOT NULL COMMENT '주문 ID (order_info.id)',
    status           VARCHAR(30) NOT NULL COMMENT '변경된 주문 상태 (PENDING_PAYMENT/PAID/CANCELLED)',
    created_datetime DATETIME(6) NOT NULL,
    updated_datetime DATETIME(6) NOT NULL
)
    COMMENT '주문 상태 변경 이력';

CREATE INDEX order_status_history_index_1
    ON order_status_history (order_info_id);


-- 기존 DB의 order_detail_info 컬럼명을 정정할 때 사용 (mysql/dev 프로파일은 ddl-auto: none 이라 수동 실행 필요)
ALTER TABLE order_detail_info
    RENAME COLUMN order_id TO order_info_id;
ALTER TABLE order_detail_info
    RENAME COLUMN product_id TO product_info_id;

-- 기존 DB의 pay_info 컬럼명을 정정할 때 사용 (mysql/dev 프로파일은 ddl-auto: none 이라 수동 실행 필요)
ALTER TABLE pay_info
    RENAME COLUMN order_id TO order_info_id;