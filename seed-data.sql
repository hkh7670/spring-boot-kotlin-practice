-- 로컬/데모 환경용 상품 데이터 시드 스크립트.
-- ddl.sql과 동일하게 "새 DB 최초 구축" 전제로 작성됨 (재실행 시 중복 insert 됨, 멱등성 보장 안 함).
-- created_datetime/updated_datetime은 BaseTimeEntity(JPA Auditing)가 채우는 컬럼이라 DB 기본값이 없어 여기서 직접 NOW()로 채운다.

INSERT INTO vendors (name, business_registration_number, contact_number, created_datetime, updated_datetime)
VALUES ('쿠팡 스타일 데모 스토어', NULL, NULL, NOW(), NOW());

INSERT INTO delivery_options (name, price, created_datetime, updated_datetime)
VALUES ('기본 배송', 3000, NOW(), NOW()),
       ('당일 배송', 5000, NOW(), NOW());

-- 대분류
INSERT INTO categories (parent_id, name, level, created_datetime, updated_datetime)
VALUES (NULL, '디지털/가전', 'LARGE', NOW(), NOW()),
       (NULL, '패션/뷰티', 'LARGE', NOW(), NOW()),
       (NULL, '식품', 'LARGE', NOW(), NOW());

-- 중분류
-- MySQL은 INSERT 대상 테이블(categories)을 같은 문장의 서브쿼리 FROM절에서 그대로 재참조하는 것을
-- 금지한다(1093 에러) — 서브쿼리를 한 번 더 파생 테이블로 감싸 미리 결과를 만들어두면(materialize)
-- 이 제약을 우회할 수 있다.
INSERT INTO categories (parent_id, name, level, created_datetime, updated_datetime)
VALUES ((SELECT id FROM (SELECT id FROM categories WHERE name = '디지털/가전' AND level = 'LARGE') AS t), '노트북/PC', 'MEDIUM', NOW(), NOW()),
       ((SELECT id FROM (SELECT id FROM categories WHERE name = '패션/뷰티' AND level = 'LARGE') AS t), '남성 의류', 'MEDIUM', NOW(), NOW()),
       ((SELECT id FROM (SELECT id FROM categories WHERE name = '식품' AND level = 'LARGE') AS t), '간편식', 'MEDIUM', NOW(), NOW());

-- 소분류 (위와 동일한 이유로 서브쿼리를 파생 테이블로 감쌈)
INSERT INTO categories (parent_id, name, level, created_datetime, updated_datetime)
VALUES ((SELECT id FROM (SELECT id FROM categories WHERE name = '노트북/PC' AND level = 'MEDIUM') AS t), '노트북', 'SMALL', NOW(), NOW()),
       ((SELECT id FROM (SELECT id FROM categories WHERE name = '노트북/PC' AND level = 'MEDIUM') AS t), '키보드/마우스', 'SMALL', NOW(), NOW()),
       ((SELECT id FROM (SELECT id FROM categories WHERE name = '남성 의류' AND level = 'MEDIUM') AS t), '상의', 'SMALL', NOW(), NOW()),
       ((SELECT id FROM (SELECT id FROM categories WHERE name = '남성 의류' AND level = 'MEDIUM') AS t), '하의', 'SMALL', NOW(), NOW()),
       ((SELECT id FROM (SELECT id FROM categories WHERE name = '간편식' AND level = 'MEDIUM') AS t), '과자/스낵', 'SMALL', NOW(), NOW()),
       ((SELECT id FROM (SELECT id FROM categories WHERE name = '간편식' AND level = 'MEDIUM') AS t), '음료', 'SMALL', NOW(), NOW());

INSERT INTO products (name, description, image_url, category_id, vendor_id, created_datetime, updated_datetime)
VALUES ('울트라 노트북 14인치', '가볍고 빠른 14인치 노트북입니다.',
        'https://picsum.photos/seed/laptop1/600/600',
        (SELECT id FROM categories WHERE name = '노트북' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('게이밍 노트북 16인치', '고성능 그래픽카드 탑재 게이밍 노트북입니다.',
        'https://picsum.photos/seed/laptop2/600/600',
        (SELECT id FROM categories WHERE name = '노트북' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('무선 기계식 키보드', '타건감이 좋은 무선 기계식 키보드입니다.',
        'https://picsum.photos/seed/keyboard1/600/600',
        (SELECT id FROM categories WHERE name = '키보드/마우스' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('인체공학 무선 마우스', '손목 부담을 줄여주는 인체공학 마우스입니다.',
        'https://picsum.photos/seed/mouse1/600/600',
        (SELECT id FROM categories WHERE name = '키보드/마우스' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('베이직 반팔 티셔츠', '부드러운 소재의 데일리 반팔 티셔츠입니다.',
        'https://picsum.photos/seed/top1/600/600',
        (SELECT id FROM categories WHERE name = '상의' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('오버핏 후드 집업', '가을/겨울 데일리 후드 집업입니다.',
        'https://picsum.photos/seed/top2/600/600',
        (SELECT id FROM categories WHERE name = '상의' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('스트레이트 데님 팬츠', '편안한 착용감의 스트레이트 핏 데님입니다.',
        'https://picsum.photos/seed/bottom1/600/600',
        (SELECT id FROM categories WHERE name = '하의' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('조거 팬츠', '활동성이 좋은 스판 조거 팬츠입니다.',
        'https://picsum.photos/seed/bottom2/600/600',
        (SELECT id FROM categories WHERE name = '하의' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('감자칩 오리지널', '바삭한 감자칩 오리지널 맛입니다.',
        'https://picsum.photos/seed/snack1/600/600',
        (SELECT id FROM categories WHERE name = '과자/스낵' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('초코 쿠키 박스', '진한 초콜릿 쿠키 10개입 박스입니다.',
        'https://picsum.photos/seed/snack2/600/600',
        (SELECT id FROM categories WHERE name = '과자/스낵' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('제로 탄산음료 24캔', '칼로리 부담 없는 제로 탄산음료 24캔 세트입니다.',
        'https://picsum.photos/seed/drink1/600/600',
        (SELECT id FROM categories WHERE name = '음료' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('콜드브루 원액 스틱', '물에 타먹는 콜드브루 원액 스틱 30개입입니다.',
        'https://picsum.photos/seed/drink2/600/600',
        (SELECT id FROM categories WHERE name = '음료' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW());

-- 상품 옵션(가격/재고는 이제 여기서만 관리) — 대부분 단일 옵션("기본")으로 기존 가격/재고를
-- 그대로 이전하고, 패션류 3개 상품만 실제 옵션 다중 분할로 데모 효과를 준다(가격은 옵션 간
-- 차등 데이터가 없어 동일 상품의 모든 옵션에 동일하게 적용).
INSERT INTO product_options (product_id, name, price, stock_count, created_datetime, updated_datetime)
VALUES ((SELECT id FROM products WHERE name = '울트라 노트북 14인치'), '기본', 1290000, 20, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '게이밍 노트북 16인치'), '기본', 2190000, 10, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '무선 기계식 키보드'), '기본', 89000, 50, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '인체공학 무선 마우스'), '기본', 39000, 60, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '베이직 반팔 티셔츠'), 'S', 19900, 25, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '베이직 반팔 티셔츠'), 'M', 19900, 40, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '베이직 반팔 티셔츠'), 'L', 19900, 35, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '오버핏 후드 집업'), '블랙', 49900, 25, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '오버핏 후드 집업'), '그레이', 49900, 15, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '스트레이트 데님 팬츠'), '30인치', 45000, 10, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '스트레이트 데님 팬츠'), '32인치', 45000, 12, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '스트레이트 데님 팬츠'), '34인치', 45000, 8, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '조거 팬츠'), '기본', 32000, 45, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '감자칩 오리지널'), '기본', 2500, 200, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '초코 쿠키 박스'), '기본', 8900, 80, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '제로 탄산음료 24캔'), '기본', 15900, 70, NOW(), NOW()),
       ((SELECT id FROM products WHERE name = '콜드브루 원액 스틱'), '기본', 12900, 90, NOW(), NOW());

-- 쿠폰/포인트 템플릿
INSERT INTO coupons (name, discount_type, discount_value, max_discount_price, min_order_price, valid_until, created_datetime, updated_datetime)
VALUES ('신규가입 5천원 할인', 'FIXED', 5000, NULL, 30000, DATE_ADD(NOW(), INTERVAL 1 YEAR), NOW(), NOW()),
       ('전상품 10% 할인', 'PERCENTAGE', 10, 10000, 0, DATE_ADD(NOW(), INTERVAL 1 YEAR), NOW(), NOW());

INSERT INTO points (name, valid_days, created_datetime, updated_datetime)
VALUES ('이벤트 지급', 365, NOW(), NOW());

-- 회원가입 API로 생성된 첫 번째 회원에게 데모용으로 쿠폰 2종 + 포인트 5000P를 발급한다.
-- 아직 가입한 회원이 없으면(최초 DB 구축 직후) 이 INSERT들은 조용히 0건 처리된다 — 회원가입 후
-- seed-data.sql만 다시 이 블록부터 재실행하면 됨(재실행 시 중복 insert되므로 한 번만 실행할 것).
INSERT INTO member_coupons (member_id, coupon_id, status, issued_at, expired_at, created_datetime, updated_datetime)
SELECT (SELECT id FROM members ORDER BY id LIMIT 1), c.id, 'UNUSED', NOW(), c.valid_until, NOW(), NOW()
FROM coupons c
WHERE EXISTS (SELECT 1 FROM members)
  AND c.name IN ('신규가입 5천원 할인', '전상품 10% 할인');

INSERT INTO member_points (member_id, point_id, amount, remaining_amount, status, issued_at, expired_at, created_datetime, updated_datetime)
SELECT (SELECT id FROM members ORDER BY id LIMIT 1), p.id, 5000, 5000, 'ACTIVE', NOW(),
       DATE_ADD(NOW(), INTERVAL p.valid_days DAY), NOW(), NOW()
FROM points p
WHERE EXISTS (SELECT 1 FROM members)
  AND p.name = '이벤트 지급';
