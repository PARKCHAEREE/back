# RDS DB Design and Collaboration Guide

## Goal
Use one shared AWS RDS MySQL database so both backend members work on identical data.

## Scope
- Keep current application tables (`power_plants`, `energy_logs`, `weather_data`, `forecasts`, `anomalies`, `vision_analyses`).
- Add schema for advisor enriched CSV (`wooyang_merged_result.csv`) with full feature retention.
- Define secure team workflow for shared DB access.

## 1) Data model decision

### 1.1 Operational time-series (app serving)
Store API-serving values in `energy_logs`.

- Primary query key: `(power_plant_id, timestamp)`
- Add weather-related optional columns in `energy_logs`:
  - `cloud_cover`, `wind_speed`, `wind_deg`, `pressure`, `dew_point`, `visibility`, `uvi`
- Keep `energy_kwh`, `temperature`, `humidity`, `irradiance` as canonical serving fields.

### 1.2 Full advisor enriched feature set (analytics/training)
Store all engineered columns in `plant_feature_logs`.

- One row per `(power_plant_id, measured_at)`
- Preserves raw+engineered columns such as `H_SIN`, `DOY_SIN`, `IRRADIANCE_PROXY`, `GEN_LAG_1`, `PREDICTION`, `ACTUAL`
- Allows reproducible retraining and feature audits

## 2) CSV-to-table mapping (advisor file)

File header sample:
`TIME,H_SIN,H_COS,DOY_SIN,...,IRRADIANCE,...,PREDICTION,ACTUAL`

Mapping rules:
- `TIME` -> `plant_feature_logs.measured_at`
- `TEMP` -> `temp`
- `HUMI` -> `humi`
- `CLOU` -> `clou`
- `WISP` -> `wisp`
- `IRRADIANCE` -> `irradiance`
- `PREDICTION` -> `prediction`
- `ACTUAL` -> `actual`
- Other engineered columns -> same-name lowercase snake-style target columns in `plant_feature_logs`

Operational projection for app APIs:
- `ACTUAL` -> `energy_logs.energy_kwh` (or `power_kw` by service convention)
- `TEMP/HUMI/IRRADIANCE/CLOU/WISP` -> corresponding `energy_logs` weather columns

## 3) SQL migration

Run SQL in:
- `docs/sql/rds_schema_v1.sql`

This script:
1. Creates `solarwise` if needed.
2. Extends `energy_logs` with optional weather/source columns.
3. Adds unique key on `(power_plant_id, timestamp)`.
4. Creates `plant_feature_logs` with full enriched features.
5. Creates `v_inference_features` view for model input.

## 4) AWS RDS setup (team-shared)

### 4.1 Create instance
- Engine: MySQL 8.0
- Instance class: `db.t3.micro` (or project budget equivalent)
- DB name: `solarwise`
- Public access: Enabled for development phase

### 4.2 Security group
- Inbound TCP `3306`
- Allow only team public IPs (recommended)

### 4.3 DB users
- `solarwise_app` (read/write for backend app)
- `solarwise_readonly` (optional for analytics/log checks)

## 5) Spring profile strategy

Use local untracked profile file for secrets:
- `src/main/resources/application-rds.properties` (not committed)

Template file to commit:
- `src/main/resources/application-rds.properties.example`

## 6) Team workflow (single source of truth)

1. One member applies SQL migration on RDS.
2. Share only endpoint + usernames via team channel.
3. Share passwords through secret manager / private channel, never in Git.
4. Everyone runs backend with `rds` profile.
5. CSV ingestion is done once per file; duplicates are blocked by unique key.

## 7) Recommended next implementation tasks

1. Extend `MeasurementService` to accept both legacy headers (`D_TEMP`) and enriched headers (`TEMP`).
2. Add `PlantFeatureLog` entity/repository and CSV loader service.
3. Add ingestion audit table (`ingestion_jobs`) for file hash, row counts, failures.
4. Add Flyway migration folder for reproducible schema evolution.

