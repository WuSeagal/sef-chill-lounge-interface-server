-- H2 seed data for local development
-- Add INSERT statements here for test data or required initial data.

-- Test admin user (Google account: seagalhsu00942@intumit.com)
INSERT INTO ADMIN_USER (
    id, provider_user_id, email, google_name,
    role_name, enabled, first_login, banned,
    created_date, last_modified_date)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '111427449810799428954',
    'seagalhsu00942@intumit.com',
    '席格',
    'ADMIN',
    TRUE, FALSE, FALSE,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- ============================================================
-- ATTENDEE_DATA (測試參加者)
-- ============================================================
INSERT INTO ATTENDEE_DATA (user_id, username, fur_name, avatar, avatar_color, topic_id, created_date, last_modified_date)
VALUES
('111427449810799428954', 'seagalhsu00942', '席格', '/user/seagal.png', '#4FC3F7', 'topic-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('demo-user-002', 'wolfie42', '小灰狼', '/user/wolfie.png', '#FF7043', 'topic-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('demo-user-003', 'foxfox', '阿狐', '/user/fox.png', '#AB47BC', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================
-- TAG (預設 TAG + 自訂 TAG)
-- is_custom=FALSE → 系統內建,GET /tags 永遠回
-- is_custom=TRUE  → 使用者自加,需 ≥5 holders 才會出現在他人可選清單
-- ============================================================
INSERT INTO TAG (tag_id, type, content, is_custom, created_date, last_modified_date)
VALUES
-- === LANGUAGE (程式語言) ===
('L001', 'LANGUAGE', 'Java', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L002', 'LANGUAGE', 'C/C++', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L003', 'LANGUAGE', 'Python', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L004', 'LANGUAGE', 'JavaScript', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L005', 'LANGUAGE', 'TypeScript', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L006', 'LANGUAGE', 'Go', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L007', 'LANGUAGE', 'PHP', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- === ROLE (角色職能) ===
('R001', 'ROLE', '前端工程師', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R002', 'ROLE', '後端工程師', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R003', 'ROLE', '全端工程師', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R004', 'ROLE', 'App工程師', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R005', 'ROLE', 'DevOps工程師', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R006', 'ROLE', '資料工程師', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R007', 'ROLE', 'UI/UX設計師', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- === PLATFORM / FRAMEWORK (平台與框架) ===
('F001', 'FRAMEWORK', 'React', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F002', 'FRAMEWORK', 'Vue', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F003', 'FRAMEWORK', 'Spring Boot', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F004', 'FRAMEWORK', 'Django', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F005', 'FRAMEWORK', 'Node.js', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F006', 'FRAMEWORK', 'iOS (Swift)', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F007', 'FRAMEWORK', 'Android (Kotlin)', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F008', 'FRAMEWORK', 'Flutter', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- === DATABASE (資料庫系統) ===
('D001', 'DATABASE', 'MySQL', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('D002', 'DATABASE', 'PostgreSQL', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('D003', 'DATABASE', 'MongoDB', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('D004', 'DATABASE', 'Redis', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- === DEVOPS / CLOUD (雲端與維運) ===
('C001', 'DEVOPS', 'AWS', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('C002', 'DEVOPS', 'Docker', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('C003', 'DEVOPS', 'Kubernetes', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('C004', 'DEVOPS', 'CI/CD', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- === CUSTOM (自訂示範) ===
-- 高人氣 demo:5 holders → 出現在他人可選清單
('CUS001', 'CUSTOM', '露營', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- 低人氣 demo:1 holder(僅 creator),不應出現
('CUS002', 'CUSTOM', '私房菜', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================
-- ATTENDEE_TAG (參加者 TAG 關聯)
-- 主測試 user 跨多個 type 各擁有 tag,讓 autofiller / preview 有料展示
-- demo-user-004 ~ demo-user-006 純作為 CUS001 (露營) 的 holders
-- ============================================================
INSERT INTO ATTENDEE_TAG (user_id, tag_id, created_date)
VALUES
-- 主測試 user(席格):程式語言 / 身分 / 框架 / 資料庫 / 雲端維運 各一個 + 自訂私房菜(只他自己)
('111427449810799428954', 'L001', CURRENT_TIMESTAMP),       -- 我寫 Java
('111427449810799428954', 'L005', CURRENT_TIMESTAMP),       -- 我寫 TypeScript
('111427449810799428954', 'R002', CURRENT_TIMESTAMP),       -- 我是 後端工程師
('111427449810799428954', 'F003', CURRENT_TIMESTAMP),       -- 我用 Spring Boot
('111427449810799428954', 'D002', CURRENT_TIMESTAMP),       -- 我存 PostgreSQL
('111427449810799428954', 'C002', CURRENT_TIMESTAMP),       -- 我會 Docker
('111427449810799428954', 'CUS002', CURRENT_TIMESTAMP),     -- 自訂:私房菜(creator)
-- demo-user-002 (小灰狼):前端 + 露營
('demo-user-002', 'L001', CURRENT_TIMESTAMP),
('demo-user-002', 'R001', CURRENT_TIMESTAMP),
('demo-user-002', 'CUS001', CURRENT_TIMESTAMP),
-- demo-user-003 (阿狐):全端 + 露營
('demo-user-003', 'R003', CURRENT_TIMESTAMP),
('demo-user-003', 'CUS001', CURRENT_TIMESTAMP),
-- demo-user-004 ~ 006:純當 CUS001 holders 湊滿 5
('demo-user-004', 'CUS001', CURRENT_TIMESTAMP),
('demo-user-005', 'CUS001', CURRENT_TIMESTAMP),
('demo-user-006', 'CUS001', CURRENT_TIMESTAMP)
;

-- ============================================================
-- ATTENDEE_SOCIAL (社交平台)
-- ============================================================
INSERT INTO ATTENDEE_SOCIAL (user_id, platform, links, created_date, last_modified_date)
VALUES
('111427449810799428954', 'X', 'https://x.com/seagal_fur', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'PLURK', 'https://www.plurk.com/seagal_fur', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('demo-user-002', 'X', 'https://x.com/wolfie42', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================
-- ATTENDEE_STICKER (個人貼圖)
-- ============================================================
INSERT INTO ATTENDEE_STICKER (user_id, sticker, created_date, last_modified_date)
VALUES
('111427449810799428954', '/sticker/seagal-1.gif', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', '/sticker/seagal-2.png', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('demo-user-002', '/sticker/wolfie-1.gif', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================
-- TOPIC (話題卡池)
-- ============================================================
INSERT INTO TOPIC (topic_id, content, created_date, last_modified_date)
VALUES
('topic-001', '你最喜歡的動物是什麼？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-002', '如果可以變成任何動物你想變成什麼？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-003', '最近在追什麼作品？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-004', '你的獸設是怎麼來的？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-005', '推薦一首最近在聽的歌！', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================
-- MESSAGES (聊天歷史 seed)
-- ============================================================
INSERT INTO MESSAGES (message_id, user_id, message_type, content, image_urls, sticker_image_url, created_date)
VALUES
('msg-seed-001', '111427449810799428954', 'TEXT', '大家好，歡迎來 chill lounge', NULL, NULL, DATEADD('MINUTE', -3, CURRENT_TIMESTAMP)),
('msg-seed-002', 'demo-user-002', 'TEXT', '這是今天拍的攤位照片', JSON '["/image/demo-1.jpg","/image/demo-2.jpg"]', NULL, DATEADD('MINUTE', -2, CURRENT_TIMESTAMP)),
('msg-seed-003', 'demo-user-003', 'STICKER', NULL, NULL, '/sticker/fox-hello.png', DATEADD('MINUTE', -1, CURRENT_TIMESTAMP))
;
