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
INSERT INTO categories (parent_id, name, level, created_datetime, updated_datetime)
VALUES ((SELECT id FROM categories WHERE name = '디지털/가전' AND level = 'LARGE'), '노트북/PC', 'MEDIUM', NOW(), NOW()),
       ((SELECT id FROM categories WHERE name = '패션/뷰티' AND level = 'LARGE'), '남성 의류', 'MEDIUM', NOW(), NOW()),
       ((SELECT id FROM categories WHERE name = '식품' AND level = 'LARGE'), '간편식', 'MEDIUM', NOW(), NOW());

-- 소분류
INSERT INTO categories (parent_id, name, level, created_datetime, updated_datetime)
VALUES ((SELECT id FROM categories WHERE name = '노트북/PC' AND level = 'MEDIUM'), '노트북', 'SMALL', NOW(), NOW()),
       ((SELECT id FROM categories WHERE name = '노트북/PC' AND level = 'MEDIUM'), '키보드/마우스', 'SMALL', NOW(), NOW()),
       ((SELECT id FROM categories WHERE name = '남성 의류' AND level = 'MEDIUM'), '상의', 'SMALL', NOW(), NOW()),
       ((SELECT id FROM categories WHERE name = '남성 의류' AND level = 'MEDIUM'), '하의', 'SMALL', NOW(), NOW()),
       ((SELECT id FROM categories WHERE name = '간편식' AND level = 'MEDIUM'), '과자/스낵', 'SMALL', NOW(), NOW()),
       ((SELECT id FROM categories WHERE name = '간편식' AND level = 'MEDIUM'), '음료', 'SMALL', NOW(), NOW());

INSERT INTO products (name, price, stock_count, description, image_url, category_id, vendor_id, created_datetime, updated_datetime)
VALUES ('울트라 노트북 14인치', 1290000, 20, '가볍고 빠른 14인치 노트북입니다.',
        'https://picsum.photos/seed/laptop1/600/600',
        (SELECT id FROM categories WHERE name = '노트북' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('게이밍 노트북 16인치', 2190000, 10, '고성능 그래픽카드 탑재 게이밍 노트북입니다.',
        'https://picsum.photos/seed/laptop2/600/600',
        (SELECT id FROM categories WHERE name = '노트북' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('무선 기계식 키보드', 89000, 50, '타건감이 좋은 무선 기계식 키보드입니다.',
        'https://picsum.photos/seed/keyboard1/600/600',
        (SELECT id FROM categories WHERE name = '키보드/마우스' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('인체공학 무선 마우스', 39000, 60, '손목 부담을 줄여주는 인체공학 마우스입니다.',
        'https://picsum.photos/seed/mouse1/600/600',
        (SELECT id FROM categories WHERE name = '키보드/마우스' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('베이직 반팔 티셔츠', 19900, 100, '부드러운 소재의 데일리 반팔 티셔츠입니다.',
        'https://picsum.photos/seed/top1/600/600',
        (SELECT id FROM categories WHERE name = '상의' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('오버핏 후드 집업', 49900, 40, '가을/겨울 데일리 후드 집업입니다.',
        'https://picsum.photos/seed/top2/600/600',
        (SELECT id FROM categories WHERE name = '상의' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('스트레이트 데님 팬츠', 45000, 30, '편안한 착용감의 스트레이트 핏 데님입니다.',
        'https://picsum.photos/seed/bottom1/600/600',
        (SELECT id FROM categories WHERE name = '하의' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('조거 팬츠', 32000, 45, '활동성이 좋은 스판 조거 팬츠입니다.',
        'https://picsum.photos/seed/bottom2/600/600',
        (SELECT id FROM categories WHERE name = '하의' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('감자칩 오리지널', 2500, 200, '바삭한 감자칩 오리지널 맛입니다.',
        'https://picsum.photos/seed/snack1/600/600',
        (SELECT id FROM categories WHERE name = '과자/스낵' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('초코 쿠키 박스', 8900, 80, '진한 초콜릿 쿠키 10개입 박스입니다.',
        'https://picsum.photos/seed/snack2/600/600',
        (SELECT id FROM categories WHERE name = '과자/스낵' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('제로 탄산음료 24캔', 15900, 70, '칼로리 부담 없는 제로 탄산음료 24캔 세트입니다.',
        'https://picsum.photos/seed/drink1/600/600',
        (SELECT id FROM categories WHERE name = '음료' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW()),
       ('콜드브루 원액 스틱', 12900, 90, '물에 타먹는 콜드브루 원액 스틱 30개입입니다.',
        'https://picsum.photos/seed/drink2/600/600',
        (SELECT id FROM categories WHERE name = '음료' AND level = 'SMALL'),
        (SELECT id FROM vendors WHERE name = '쿠팡 스타일 데모 스토어'), NOW(), NOW());
