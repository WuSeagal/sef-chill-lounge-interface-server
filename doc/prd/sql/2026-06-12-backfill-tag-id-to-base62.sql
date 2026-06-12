-- Production backfill for legacy tag_id values:
--   R001 / L001 / F001 / D001 / C001 / CUS001
-- to:
--   R00001 / L00001 / F00001 / D00001 / C00001 / CUS00001
--
-- Scope:
--   1. TAG.tag_id
--   2. ATTENDEE_TAG.tag_id
--
-- Safety:
--   - Only rows matching the legacy numeric format are touched.
--   - Run inside a transaction.
--   - Preview queries are included before COMMIT.
--
-- Expected on production:
--   database: PostgreSQL

BEGIN;

-- Convert decimal number to base62 (0-9, a-z, A-Z), then left-pad to 5 chars.
CREATE OR REPLACE FUNCTION public.sef_to_base62_padded_5(input_num bigint)
RETURNS text
LANGUAGE plpgsql
AS $$
DECLARE
    alphabet constant text := '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
    value_num bigint := input_num;
    result_text text := '';
    remainder_num integer;
BEGIN
    IF value_num IS NULL OR value_num < 0 THEN
        RAISE EXCEPTION 'input_num must be >= 0';
    END IF;

    IF value_num = 0 THEN
        result_text := '0';
    ELSE
        WHILE value_num > 0 LOOP
            remainder_num := (value_num % 62)::integer;
            result_text := substr(alphabet, remainder_num + 1, 1) || result_text;
            value_num := value_num / 62;
        END LOOP;
    END IF;

    IF length(result_text) > 5 THEN
        RAISE EXCEPTION 'base62 result "%" exceeds 5 chars for input %', result_text, input_num;
    END IF;

    RETURN lpad(result_text, 5, '0');
END;
$$;

-- Build deterministic old->new mapping only for legacy IDs.
CREATE TEMP TABLE tmp_tag_id_mapping AS
WITH legacy_tags AS (
    SELECT
        t.id,
        t.tag_id AS old_tag_id,
        t.type,
        CASE
            WHEN t.tag_id ~ '^CUS[0-9]+$' THEN 'CUS'
            WHEN t.tag_id ~ '^R[0-9]+$' THEN 'R'
            WHEN t.tag_id ~ '^L[0-9]+$' THEN 'L'
            WHEN t.tag_id ~ '^F[0-9]+$' THEN 'F'
            WHEN t.tag_id ~ '^D[0-9]+$' THEN 'D'
            WHEN t.tag_id ~ '^C[0-9]+$' THEN 'C'
            ELSE NULL
        END AS prefix,
        CASE
            WHEN t.tag_id ~ '^CUS[0-9]+$' THEN substring(t.tag_id FROM 4)::bigint
            WHEN t.tag_id ~ '^[RLFDC][0-9]+$' THEN substring(t.tag_id FROM 2)::bigint
            ELSE NULL
        END AS numeric_value
    FROM tag t
)
SELECT
    id,
    old_tag_id,
    prefix || public.sef_to_base62_padded_5(numeric_value) AS new_tag_id
FROM legacy_tags
WHERE prefix IS NOT NULL
  AND numeric_value IS NOT NULL
  AND old_tag_id <> prefix || public.sef_to_base62_padded_5(numeric_value);

-- Guard: no duplicate target IDs should be produced.
DO $$
BEGIN
    IF EXISTS (
        SELECT new_tag_id
        FROM tmp_tag_id_mapping
        GROUP BY new_tag_id
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate target tag_id detected in tmp_tag_id_mapping';
    END IF;
END;
$$;

-- Guard: target IDs must not already exist outside the rows we are updating.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM tmp_tag_id_mapping m
        JOIN tag t
          ON t.tag_id = m.new_tag_id
         AND t.id <> m.id
    ) THEN
        RAISE EXCEPTION 'Target tag_id already exists on another row';
    END IF;
END;
$$;

-- Preview mapping before update.
SELECT *
FROM tmp_tag_id_mapping
ORDER BY old_tag_id;

-- Update child table first.
UPDATE attendee_tag at
SET tag_id = m.new_tag_id
FROM tmp_tag_id_mapping m
WHERE at.tag_id = m.old_tag_id;

-- Then update source table.
UPDATE tag t
SET tag_id = m.new_tag_id
FROM tmp_tag_id_mapping m
WHERE t.id = m.id;

-- Post-check preview.
SELECT
    'tag_rows_updated' AS check_name,
    count(*) AS row_count
FROM tmp_tag_id_mapping
UNION ALL
SELECT
    'legacy_tag_rows_remaining',
    count(*)
FROM tag
WHERE tag_id ~ '^(CUS|R|L|F|D|C)[0-9]{3,}$'
  AND tag_id !~ '^(CUS|R|L|F|D|C)[0-9a-zA-Z]{5}$';

SELECT tag_id, type, content
FROM tag
ORDER BY type, tag_id
LIMIT 50;

-- If previews look correct, replace the next line with COMMIT and rerun the script.
ROLLBACK;

-- Optional cleanup if you want to remove the helper after verification:
-- DROP FUNCTION IF EXISTS public.sef_to_base62_padded_5(bigint);
