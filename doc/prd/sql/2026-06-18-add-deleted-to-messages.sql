-- 2026-06-18 delete-chat-message change
-- 正式 PostgreSQL：為 MESSAGES 新增 soft-delete 旗標。
-- ddl-auto: none，故須於部署前手動執行此腳本。
-- 既有資料列由 DEFAULT FALSE 填補，無需 backfill。

ALTER TABLE MESSAGES ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
