-- SolarWise RDS schema (MySQL 8)
-- Purpose: shared DB for backend team + advisor enriched CSV ingestion

CREATE DATABASE IF NOT EXISTS solarwise
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE solarwise;

-- 1) Existing core table extensions (safe with IF NOT EXISTS in migration tools)
ALTER TABLE energy_logs
  ADD COLUMN IF NOT EXISTS cloud_cover DOUBLE NULL,
  ADD COLUMN IF NOT EXISTS wind_speed DOUBLE NULL,
  ADD COLUMN IF NOT EXISTS wind_deg DOUBLE NULL,
  ADD COLUMN IF NOT EXISTS pressure DOUBLE NULL,
  ADD COLUMN IF NOT EXISTS dew_point DOUBLE NULL,
  ADD COLUMN IF NOT EXISTS visibility DOUBLE NULL,
  ADD COLUMN IF NOT EXISTS uvi DOUBLE NULL,
  ADD COLUMN IF NOT EXISTS source_file VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS source_row_no INT NULL;

-- Prevent duplicate records for the same plant and timestamp
ALTER TABLE energy_logs
  ADD UNIQUE KEY IF NOT EXISTS uk_energy_logs_plant_timestamp (power_plant_id, timestamp);

-- 2) New table: keep full enriched feature set from advisor CSV
CREATE TABLE IF NOT EXISTS plant_feature_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  power_plant_id BIGINT NOT NULL,

  measured_at DATETIME NOT NULL,

  -- Core weather / generation columns
  temp DOUBLE NULL,
  humi DOUBLE NULL,
  clou DOUBLE NULL,
  wisp DOUBLE NULL,
  irradiance DOUBLE NULL,
  est_irradiance DOUBLE NULL,
  actual DOUBLE NULL,
  prediction DOUBLE NULL,

  -- Engineered features (from merged CSV)
  h_sin DOUBLE NULL,
  h_cos DOUBLE NULL,
  doy_sin DOUBLE NULL,
  doy_cos DOUBLE NULL,
  wide_sin DOUBLE NULL,
  wide_cos DOUBLE NULL,
  sun_elev_clip DOUBLE NULL,
  cos_zen DOUBLE NULL,
  irradiance_proxy DOUBLE NULL,
  irradiance_x_capa DOUBLE NULL,
  capa DOUBLE NULL,
  seasonal_solar_pattern DOUBLE NULL,
  weather_adjusted_pattern DOUBLE NULL,
  expected_gen_proxy DOUBLE NULL,
  gen_lag_1 DOUBLE NULL,
  gen_lag_2 DOUBLE NULL,
  gen_roll_mean_6 DOUBLE NULL,

  source_file VARCHAR(255) NULL,
  source_row_no INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_feature_logs_power_plant
    FOREIGN KEY (power_plant_id) REFERENCES power_plants(id),

  CONSTRAINT uk_feature_logs_plant_measured_at
    UNIQUE (power_plant_id, measured_at)
);

CREATE INDEX idx_feature_logs_plant_time
  ON plant_feature_logs (power_plant_id, measured_at);

CREATE INDEX idx_feature_logs_time
  ON plant_feature_logs (measured_at);

-- Optional view: unified inference input from feature logs
CREATE OR REPLACE VIEW v_inference_features AS
SELECT
  pfl.power_plant_id,
  pfl.measured_at,
  pfl.temp,
  pfl.humi,
  pfl.clou AS cloud_cover,
  pfl.wisp AS wind_speed,
  pfl.irradiance,
  pfl.actual,
  pfl.prediction
FROM plant_feature_logs pfl;

