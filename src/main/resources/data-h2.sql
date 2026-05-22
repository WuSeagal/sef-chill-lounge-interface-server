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
VALUES ('111427449810799428954', 'seagalhsu00942', '席格', '/uploads/avatar/seagal.png', '#4FC3F7', 'topic-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO ATTENDEE_DATA (user_id, username, fur_name, avatar, avatar_color, topic_id, created_date, last_modified_date)
VALUES ('demo-user-002', 'wolfie42', '小灰狼', '/uploads/avatar/wolfie.png', '#FF7043', 'topic-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO ATTENDEE_DATA (user_id, username, fur_name, avatar, avatar_color, topic_id, created_date, last_modified_date)
VALUES ('demo-user-003', 'foxfox', '阿狐', '/uploads/avatar/fox.png', '#AB47BC', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================
-- TAG (預設 TAG + 自訂 TAG)
-- ============================================================
INSERT INTO TAG (tag_id, type, content, created_date, last_modified_date)
VALUES ('tag-def-001', 'species', '狼', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO TAG (tag_id, type, content, created_date, last_modified_date)
VALUES ('tag-def-002', 'species', '狐狸', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO TAG (tag_id, type, content, created_date, last_modified_date)
VALUES ('tag-def-003', 'species', '龍', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO TAG (tag_id, type, content, created_date, last_modified_date)
VALUES ('tag-def-004', 'hobby', '畫畫', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO TAG (tag_id, type, content, created_date, last_modified_date)
VALUES ('tag-def-005', 'hobby', '寫作', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO TAG (tag_id, type, content, created_date, last_modified_date)
VALUES ('tag-cus-001', 'custom', '喜歡吃拉麵', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================
-- ATTENDEE_TAG (參加者 TAG 關聯)
-- ============================================================
INSERT INTO ATTENDEE_TAG (user_id, tag_id, created_date)
VALUES ('111427449810799428954', 'tag-def-001', CURRENT_TIMESTAMP);

INSERT INTO ATTENDEE_TAG (user_id, tag_id, created_date)
VALUES ('111427449810799428954', 'tag-def-004', CURRENT_TIMESTAMP);

INSERT INTO ATTENDEE_TAG (user_id, tag_id, created_date)
VALUES ('demo-user-002', 'tag-def-001', CURRENT_TIMESTAMP);

INSERT INTO ATTENDEE_TAG (user_id, tag_id, created_date)
VALUES ('demo-user-002', 'tag-cus-001', CURRENT_TIMESTAMP);

INSERT INTO ATTENDEE_TAG (user_id, tag_id, created_date)
VALUES ('demo-user-003', 'tag-def-002', CURRENT_TIMESTAMP);

-- ============================================================
-- ATTENDEE_SOCIAL (社交平台)
-- ============================================================
INSERT INTO ATTENDEE_SOCIAL (user_id, platform, links, created_date, last_modified_date)
VALUES ('111427449810799428954', 'twitter', 'https://twitter.com/seagal_fur', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO ATTENDEE_SOCIAL (user_id, platform, links, created_date, last_modified_date)
VALUES ('111427449810799428954', 'plurk', 'https://www.plurk.com/seagal_fur', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO ATTENDEE_SOCIAL (user_id, platform, links, created_date, last_modified_date)
VALUES ('demo-user-002', 'twitter', 'https://twitter.com/wolfie42', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================
-- ATTENDEE_STICKER (個人貼圖)
-- ============================================================
INSERT INTO ATTENDEE_STICKER (user_id, sticker_no, sticker, created_date, last_modified_date)
VALUES ('111427449810799428954', 1, '/uploads/sticker/seagal-1.gif', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO ATTENDEE_STICKER (user_id, sticker_no, sticker, created_date, last_modified_date)
VALUES ('111427449810799428954', 2, '/uploads/sticker/seagal-2.png', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO ATTENDEE_STICKER (user_id, sticker_no, sticker, created_date, last_modified_date)
VALUES ('demo-user-002', 1, '/uploads/sticker/wolfie-1.gif', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================
-- TOPIC (話題卡池)
-- ============================================================
INSERT INTO TOPIC (topic_id, content, created_date, last_modified_date)
VALUES ('topic-001', '你最喜歡的動物是什麼？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO TOPIC (topic_id, content, created_date, last_modified_date)
VALUES ('topic-002', '如果可以變成任何動物你想變成什麼？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO TOPIC (topic_id, content, created_date, last_modified_date)
VALUES ('topic-003', '最近在追什麼作品？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO TOPIC (topic_id, content, created_date, last_modified_date)
VALUES ('topic-004', '你的獸設是怎麼來的？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO TOPIC (topic_id, content, created_date, last_modified_date)
VALUES ('topic-005', '推薦一首最近在聽的歌！', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
