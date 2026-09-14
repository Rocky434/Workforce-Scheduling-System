CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('EMPLOYEE', 'OWNER')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE calendar_days (
    calendar_date DATE PRIMARY KEY,
    day_type VARCHAR(20) NOT NULL CHECK (day_type IN ('WORKDAY', 'WEEKEND', 'HOLIDAY')),
    holiday_name VARCHAR(100),
    schedulable BOOLEAN NOT NULL,
    source VARCHAR(200) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE schedule_entries (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES users(id),
    work_date DATE NOT NULL REFERENCES calendar_days(calendar_date),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_employee_work_date UNIQUE (employee_id, work_date)
);

CREATE INDEX idx_schedule_work_date ON schedule_entries(work_date);
CREATE INDEX idx_schedule_employee_date ON schedule_entries(employee_id, work_date);

