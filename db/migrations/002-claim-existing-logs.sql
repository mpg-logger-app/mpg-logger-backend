-- Upgrade, step 2 of 2: gives the fuel logs from before accounts existed to the first account.
-- Run this once, AFTER deploying the new backend and signing up. Safe to run more than once.
--
-- It only claims logs while there is exactly one account (yours), so it can never hand your
-- data to someone else. Then it makes user_id required. If that last statement fails with
-- "contains null values", there were no accounts or more than one: check app_user and set
-- fuel_log.user_id by hand.
-- No blank lines inside a statement: DBeaver treats a blank line as the end of a statement.

UPDATE mpglogger.fuel_log
SET    user_id = (SELECT id FROM mpglogger.app_user)
WHERE  user_id IS NULL
  AND  (SELECT count(*) FROM mpglogger.app_user) = 1;

ALTER TABLE mpglogger.fuel_log ALTER COLUMN user_id SET NOT NULL;