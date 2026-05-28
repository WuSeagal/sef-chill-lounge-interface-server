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
-- ============================================================
INSERT INTO TAG (tag_id, type, content, created_date, last_modified_date)
VALUES
-- === LANGUAGE (程式語言) ===
('L001', 'LANGUAGE', 'Java', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L002', 'LANGUAGE', 'C/C++', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L003', 'LANGUAGE', 'Python', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L004', 'LANGUAGE', 'JavaScript', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L005', 'LANGUAGE', 'TypeScript', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L006', 'LANGUAGE', 'Go', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L007', 'LANGUAGE', 'PHP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- === ROLE (角色職能) ===
('R001', 'ROLE', '前端工程師', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R002', 'ROLE', '後端工程師', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R003', 'ROLE', '全端工程師', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R004', 'ROLE', 'App工程師', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R005', 'ROLE', 'DevOps工程師', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R006', 'ROLE', '資料工程師', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R007', 'ROLE', 'UI/UX設計師', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- === PLATFORM / FRAMEWORK (平台與框架) ===
('F001', 'FRAMEWORK', 'React', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F002', 'FRAMEWORK', 'Vue', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F003', 'FRAMEWORK', 'Spring Boot', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F004', 'FRAMEWORK', 'Django', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F005', 'FRAMEWORK', 'Node.js', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F006', 'FRAMEWORK', 'iOS (Swift)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F007', 'FRAMEWORK', 'Android (Kotlin)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F008', 'FRAMEWORK', 'Flutter', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- === DATABASE (資料庫系統) ===
('D001', 'DATABASE', 'MySQL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('D002', 'DATABASE', 'PostgreSQL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('D003', 'DATABASE', 'MongoDB', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('D004', 'DATABASE', 'Redis', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- === DEVOPS / CLOUD (雲端與維運) ===
('C001', 'DEVOPS', 'AWS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('C002', 'DEVOPS', 'Docker', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('C003', 'DEVOPS', 'Kubernetes', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('C004', 'DEVOPS', 'CI/CD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================
-- ATTENDEE_TAG (參加者 TAG 關聯)
-- ============================================================
INSERT INTO ATTENDEE_TAG (user_id, tag_id, created_date)
VALUES
('111427449810799428954', 'tag-def-001', CURRENT_TIMESTAMP),
('111427449810799428954', 'tag-def-004', CURRENT_TIMESTAMP),
('demo-user-002', 'tag-def-001', CURRENT_TIMESTAMP),
('demo-user-002', 'tag-cus-001', CURRENT_TIMESTAMP),
('demo-user-003', 'tag-def-002', CURRENT_TIMESTAMP)
;

-- ============================================================
-- ATTENDEE_SOCIAL (社交平台)
-- ============================================================
INSERT INTO ATTENDEE_SOCIAL (user_id, platform, links, created_date, last_modified_date)
VALUES
('111427449810799428954', 'twitter', 'https://twitter.com/seagal_fur', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'plurk', 'https://www.plurk.com/seagal_fur', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('demo-user-002', 'twitter', 'https://twitter.com/wolfie42', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================
-- ATTENDEE_STICKER (個人貼圖)
-- ============================================================
INSERT INTO ATTENDEE_STICKER (user_id, sticker_no, sticker, created_date, last_modified_date)
VALUES
('111427449810799428954', 1, '/sticker/seagal-1.gif', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 2, '/sticker/seagal-2.png', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('demo-user-002', 1, '/sticker/wolfie-1.gif', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
