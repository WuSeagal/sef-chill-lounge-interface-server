-- H2 seed data for local development
-- Add INSERT statements here for test data or required initial data.

-- Test admin user (Google account: seagalhsu00942@intumit.com)
-- Fake Data
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
-- Fake Data
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
('L00001', 'LANGUAGE', 'Java', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L00002', 'LANGUAGE', 'C/C++', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L00003', 'LANGUAGE', 'Python', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L00004', 'LANGUAGE', 'JavaScript', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L00005', 'LANGUAGE', 'TypeScript', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L00006', 'LANGUAGE', 'Go', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('L00007', 'LANGUAGE', 'PHP', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- === ROLE (角色職能) ===
('R00001', 'ROLE', '前端工程師', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R00002', 'ROLE', '後端工程師', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R00003', 'ROLE', '全端工程師', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R00004', 'ROLE', 'App工程師', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R00005', 'ROLE', 'DevOps工程師', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R00006', 'ROLE', '資料工程師', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('R00007', 'ROLE', 'UI/UX設計師', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- === PLATFORM / FRAMEWORK (平台與框架) ===
('F00001', 'FRAMEWORK', 'React', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F00002', 'FRAMEWORK', 'Vue', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F00003', 'FRAMEWORK', 'Spring Boot', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F00004', 'FRAMEWORK', 'Django', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F00005', 'FRAMEWORK', 'Node.js', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F00006', 'FRAMEWORK', 'iOS (Swift)', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F00007', 'FRAMEWORK', 'Android (Kotlin)', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('F00008', 'FRAMEWORK', 'Flutter', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- === DATABASE (資料庫系統) ===
('D00001', 'DATABASE', 'MySQL', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('D00002', 'DATABASE', 'PostgreSQL', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('D00003', 'DATABASE', 'MongoDB', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('D00004', 'DATABASE', 'Redis', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- === DEVOPS / CLOUD (雲端與維運) ===
('C00001', 'DEVOPS', 'AWS', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('C00002', 'DEVOPS', 'Docker', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('C00003', 'DEVOPS', 'Kubernetes', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('C00004', 'DEVOPS', 'CI/CD', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- === CUSTOM (自訂示範) ===
-- Fake Data
-- 高人氣 demo:5 holders → 出現在他人可選清單
('CUS00001', 'CUSTOM', '露營', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- 低人氣 demo:1 holder(僅 creator),不應出現
('CUS00002', 'CUSTOM', '私房菜', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================
-- ATTENDEE_TAG (參加者 TAG 關聯)
-- 主測試 user 跨多個 type 各擁有 tag,讓 autofiller / preview 有料展示
-- demo-user-004 ~ demo-user-006 純作為 CUS001 (露營) 的 holders
-- ============================================================
-- Fake Data
INSERT INTO ATTENDEE_TAG (user_id, tag_id, created_date)
VALUES
-- 主測試 user(席格):程式語言 / 身分 / 框架 / 資料庫 / 雲端維運 各一個 + 自訂私房菜(只他自己)
('111427449810799428954', 'L00001', CURRENT_TIMESTAMP),       -- 我寫 Java
('111427449810799428954', 'L00005', CURRENT_TIMESTAMP),       -- 我寫 TypeScript
('111427449810799428954', 'R00002', CURRENT_TIMESTAMP),       -- 我是 後端工程師
('111427449810799428954', 'F00003', CURRENT_TIMESTAMP),       -- 我用 Spring Boot
('111427449810799428954', 'D00002', CURRENT_TIMESTAMP),       -- 我存 PostgreSQL
('111427449810799428954', 'C00002', CURRENT_TIMESTAMP),       -- 我會 Docker
('111427449810799428954', 'CUS00002', CURRENT_TIMESTAMP),     -- 自訂:私房菜(creator)
-- demo-user-002 (小灰狼):前端 + 露營
('demo-user-002', 'L00001', CURRENT_TIMESTAMP),
('demo-user-002', 'R00001', CURRENT_TIMESTAMP),
('demo-user-002', 'CUS00001', CURRENT_TIMESTAMP),
-- demo-user-003 (阿狐):全端 + 露營
('demo-user-003', 'R00003', CURRENT_TIMESTAMP),
('demo-user-003', 'CUS00001', CURRENT_TIMESTAMP),
-- demo-user-004 ~ 006:純當 CUS001 holders 湊滿 5
('demo-user-004', 'CUS00001', CURRENT_TIMESTAMP),
('demo-user-005', 'CUS00001', CURRENT_TIMESTAMP),
('demo-user-006', 'CUS00001', CURRENT_TIMESTAMP)
;

-- ============================================================
-- ATTENDEE_SOCIAL (社交平台)
-- ============================================================
-- Fake Data
INSERT INTO ATTENDEE_SOCIAL (user_id, platform, links, created_date, last_modified_date)
VALUES
-- 席格:各平台展示資料（URL 皆符合各平台 host pattern 與輸入模板；DISCORD 此筆為伺服器邀請故用 DISCORD_SERVER）
('111427449810799428954', 'FACEBOOK',   'https://www.facebook.com/seagal.fur',          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'STEAM',      'https://steamcommunity.com/profiles/76561198000000000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'PLURK',      'https://www.plurk.com/seagal_fur',             CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'CAKERESUME', 'https://www.cake.me/me/seagal-fur',            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'LINKEDIN',   'https://www.linkedin.com/in/seagalfur',        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'TWITCH',     'https://www.twitch.tv/seagalfur',              CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'THREADS',    'https://www.threads.com/@seagal_fur',          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'INSTAGRAM',  'https://www.instagram.com/seagal_fur',         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'DISCORD_SERVER', 'https://discord.gg/seagalfur',             CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'BLUESKY',    'https://bsky.app/profile/seagal.bsky.social',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'X',          'https://x.com/seagal_fur',                     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'GITHUB',     'https://github.com/seagalfur',                 CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'PERSONAL',   'https://seagal.dev',                           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('111427449810799428954', 'OTHER',      'https://linktr.ee/seagalfur',                  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('demo-user-002', 'X', 'https://x.com/wolfie42', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================
-- ATTENDEE_STICKER (個人貼圖)
-- ============================================================
-- Fake Data
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
('topic-001', '你的獸設和你的職業有沒有共通點？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-002', '你的Side Project或個人頁面有沒有獸相關元素？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-003', '你為什麼會想接觸軟體業？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-004', '你最初接觸的程式語言？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-005', '除了上班用到的程式語言，你有沒有其他喜歡或擅長的程式語言？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-006', '你有沒有個人網頁？有沒有很酷的畫面設計？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-007', '你有和其他獸圈朋友一起開發過、或想開發什麼專案嗎？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-008', '你有在工作中使用什麼AI服務嗎？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-009', '你知道美味蟹保的祖傳祕方是什麼嗎？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-010', '這次UTFG你最期待哪個活動？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-011', '你是什麼時候進入獸圈的？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-012', '你是什麼時候開始學程式的？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-013', '你在獸圈最喜歡的人？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-014', '你的第一個程式專案成品是什麼？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-015', '你的獸設或名字和程式設計有沒有關聯？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-016', '你有聽過Lizardchi嗎？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-017', '你有委託或畫過和程式設計相關的圖嗎？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-018', '你有在你設計的程式裡面放過什麼有趣的彩蛋？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-019', '你在邊寫程式最常邊吃什麼東西？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('topic-020', '如果你有一個零食櫃，裡面會放什麼？', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
;

-- ============================================================
-- MESSAGES (聊天歷史 seed)
-- 時間跨度拉大（7 天前 ~ 剛剛），供前端時間戳（今天/昨天/更早）與跨日分隔線測試；
-- 並含數筆回覆（reply_to_message_id 指向本區塊內既有訊息），其中一筆回覆 7 天前的訊息，
-- 供測試「跳轉載入到底」（jump-load 需連續往回載入歷史才能找到目標）。
-- 回覆的作者名字/內容摘要/建立時間一律讀取時即時解析，不落庫，故此處只需設定 reply_to_message_id。
-- ============================================================
-- Fake Data
INSERT INTO MESSAGES (message_id, user_id, message_type, content, image_urls, sticker_image_url, created_date, reply_to_message_id)
VALUES
-- 7 天前
('260601090001Seed0001', '111427449810799428954', 'TEXT', '哇這是上上禮拜的訊息了', NULL, NULL, DATEADD('DAY', -7, CURRENT_TIMESTAMP), NULL),
('260601090002Seed0002', 'demo-user-002', 'TEXT', '對啊時間過得好快', NULL, NULL, DATEADD('MINUTE', 5, DATEADD('DAY', -7, CURRENT_TIMESTAMP)), NULL),
-- 5 天前
('260603100003Seed0003', 'demo-user-003', 'TEXT', '上次那個活動好好玩，附上照片', JSON '["/image/demo-3.jpg"]', NULL, DATEADD('DAY', -5, CURRENT_TIMESTAMP), NULL),
-- 3 天前
('260605110004Seed0004', '111427449810799428954', 'TEXT', '剛剛整理了一下攤位設備', NULL, NULL, DATEADD('DAY', -3, CURRENT_TIMESTAMP), NULL),
('260605110005Seed0005', 'demo-user-002', 'TEXT', '辛苦了！', NULL, NULL, DATEADD('MINUTE', 10, DATEADD('DAY', -3, CURRENT_TIMESTAMP)), '260605110004Seed0004'),
-- 昨天
('260607080006Seed0006', 'demo-user-003', 'TEXT', '明天要早起集合', NULL, NULL, DATEADD('DAY', -1, CURRENT_TIMESTAMP), NULL),
-- 昨天，回覆 7 天前的訊息（測試 jump-load 需往回撈多批歷史）
('260607090007Seed0007', '111427449810799428954', 'TEXT', '我還記得那次，超懷念的！', NULL, NULL, DATEADD('HOUR', 1, DATEADD('DAY', -1, CURRENT_TIMESTAMP)), '260601090001Seed0001'),
-- 今天，較早
('260608060008Seed0008', 'demo-user-002', 'TEXT', '早安大家', NULL, NULL, DATEADD('HOUR', -6, CURRENT_TIMESTAMP), NULL),
('260608070009Seed0009', 'demo-user-003', 'TEXT', '早安！', NULL, NULL, DATEADD('HOUR', -5, CURRENT_TIMESTAMP), NULL),
('260608080010Seed0010', '111427449810799428954', 'STICKER', NULL, NULL, '/sticker/seagal-1.gif', DATEADD('HOUR', -4, CURRENT_TIMESTAMP), NULL),
('260608090011Seed0011', 'demo-user-002', 'TEXT', '這次UTFG的攤位好多，逛不完', NULL, NULL, DATEADD('HOUR', -3, CURRENT_TIMESTAMP), NULL),
('260608090012Seed0012', 'demo-user-003', 'TEXT', '對啊我剛逛了一圈', NULL, NULL, DATEADD('MINUTE', 2, DATEADD('HOUR', -3, CURRENT_TIMESTAMP)), '260608090011Seed0011'),
('260608100013Seed0013', '111427449810799428954', 'TEXT', '中午的合照', JSON '["/image/demo-4.jpg","/image/demo-5.jpg"]', NULL, DATEADD('HOUR', -2, CURRENT_TIMESTAMP), NULL),
('260608110014Seed0014', 'demo-user-002', 'TEXT', '午餐大家吃什麼', NULL, NULL, DATEADD('MINUTE', -90, CURRENT_TIMESTAMP), NULL),
('260608110015Seed0015', 'demo-user-003', 'TEXT', '便當如何？附近那家不錯', NULL, NULL, DATEADD('MINUTE', -80, CURRENT_TIMESTAMP), '260608110014Seed0014'),
-- 剛剛（既有 3 筆，維持不變）
('260612120001AbCd1234', '111427449810799428954', 'TEXT', '大家好，歡迎來 chill lounge', NULL, NULL, DATEADD('MINUTE', -3, CURRENT_TIMESTAMP), NULL),
('260612120002BcDe2345', 'demo-user-002', 'TEXT', '這是今天拍的攤位照片', JSON '["/image/demo-1.jpg","/image/demo-2.jpg"]', NULL, DATEADD('MINUTE', -2, CURRENT_TIMESTAMP), NULL),
('260612120003CdEf3456', 'demo-user-003', 'STICKER', NULL, NULL, '/sticker/fox-hello.png', DATEADD('MINUTE', -1, CURRENT_TIMESTAMP), NULL)
;
