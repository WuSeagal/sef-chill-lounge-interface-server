-- 2026-07-09 chat-message-enhancements change
-- 正式 PostgreSQL：為 MESSAGES 新增回覆關聯欄位（唯一持久化的回覆欄位，僅存 messageId）。
-- ddl-auto: none，故須於部署前手動執行此腳本。
-- 既有資料列由 NULL 填補（非回覆訊息），無需 backfill。

ALTER TABLE MESSAGES ADD COLUMN IF NOT EXISTS reply_to_message_id VARCHAR(64);
