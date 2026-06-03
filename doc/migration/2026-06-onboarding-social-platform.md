# 社群平台 String → PlatformEnum 資料遷移

## 背景

`onboarding-redesign` change 將 `ATTENDEE_SOCIAL.platform` 由自由字串收斂為固定 `PlatformEnum`（`@Enumerated(EnumType.STRING)`，仍以 varchar 存 enum name）。14 個合法值（大寫）：

```
FACEBOOK, STEAM, PLURK, CAKERESUME, LINKEDIN, TWITCH, THREADS,
INSTAGRAM, DISCORD, BLUESKY, X, GITHUB, PERSONAL, OTHER
```

> 不含 Email。

## Dev（H2 in-memory）

無需手動遷移：H2 每次啟動由 `data-h2.sql` 重建，seed 內 social platform 值已更新為合法 enum name（如 `X`、`PLURK`）。

## Production（PostgreSQL）

`ATTENDEE_SOCIAL.platform` 欄位型別不變（varchar），但既有舊值（多為小寫自由字串）需在**部署新版（會以 enum 讀取）之前**轉成大寫 enum name；無法對照者一律轉 `OTHER`，避免 `@Enumerated(STRING)` 讀取時 `IllegalArgumentException`。

### 遷移 SQL

```sql
-- 1) 已知舊值對照（依實際 prd 既有資料增補）
UPDATE ATTENDEE_SOCIAL SET platform = 'X'          WHERE LOWER(platform) IN ('x', 'twitter');
UPDATE ATTENDEE_SOCIAL SET platform = 'FACEBOOK'   WHERE LOWER(platform) IN ('facebook', 'fb');
UPDATE ATTENDEE_SOCIAL SET platform = 'INSTAGRAM'  WHERE LOWER(platform) = 'instagram';
UPDATE ATTENDEE_SOCIAL SET platform = 'PLURK'      WHERE LOWER(platform) = 'plurk';
UPDATE ATTENDEE_SOCIAL SET platform = 'GITHUB'     WHERE LOWER(platform) = 'github';
UPDATE ATTENDEE_SOCIAL SET platform = 'LINKEDIN'   WHERE LOWER(platform) = 'linkedin';
UPDATE ATTENDEE_SOCIAL SET platform = 'TWITCH'     WHERE LOWER(platform) = 'twitch';
UPDATE ATTENDEE_SOCIAL SET platform = 'THREADS'    WHERE LOWER(platform) = 'threads';
UPDATE ATTENDEE_SOCIAL SET platform = 'DISCORD'    WHERE LOWER(platform) = 'discord';
UPDATE ATTENDEE_SOCIAL SET platform = 'BLUESKY'    WHERE LOWER(platform) IN ('bluesky', 'bsky');
UPDATE ATTENDEE_SOCIAL SET platform = 'STEAM'      WHERE LOWER(platform) = 'steam';
UPDATE ATTENDEE_SOCIAL SET platform = 'PLURK'      WHERE LOWER(platform) = 'plurk';
UPDATE ATTENDEE_SOCIAL SET platform = 'CAKERESUME' WHERE LOWER(platform) IN ('cakeresume', 'cake');

-- 2) 其餘無法對照者一律轉 OTHER（含舊的 'fa'、'personal'、任意字串）
UPDATE ATTENDEE_SOCIAL SET platform = 'OTHER'
 WHERE platform NOT IN (
   'FACEBOOK','STEAM','PLURK','CAKERESUME','LINKEDIN','TWITCH','THREADS',
   'INSTAGRAM','DISCORD','BLUESKY','X','GITHUB','PERSONAL','OTHER'
 );
```

### 驗證

```sql
-- 應回傳 0 列（無非法值殘留）
SELECT DISTINCT platform FROM ATTENDEE_SOCIAL
 WHERE platform NOT IN (
   'FACEBOOK','STEAM','PLURK','CAKERESUME','LINKEDIN','TWITCH','THREADS',
   'INSTAGRAM','DISCORD','BLUESKY','X','GITHUB','PERSONAL','OTHER'
 );
```

> 注意：`links` 欄位不遷移；新版起 `links` 於新增/更新時才跑兩層 URL 驗證，既有資料不回溯驗證。
