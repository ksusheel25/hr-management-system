CREATE TABLE company (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    timezone VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_company_company_id ON company (company_id);

CREATE TABLE employee (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    company_ref_id UUID NOT NULL,
    employee_no VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uq_employee_company_employee_no UNIQUE (company_id, employee_no)
);

CREATE INDEX idx_employee_company_id ON employee (company_id);

CREATE TABLE shift (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_shift_company_id ON shift (company_id);

CREATE TABLE attendance_event (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    device_log_id VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uq_attendance_event_device_log_id UNIQUE (device_log_id)
);

CREATE INDEX idx_attendance_event_company_id ON attendance_event (company_id);
CREATE INDEX idx_attendance_event_employee_time ON attendance_event (company_id, employee_id, event_time);

CREATE TABLE daily_summary (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    summary_date DATE NOT NULL,
    present_minutes INTEGER NOT NULL,
    overtime_minutes INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_daily_summary_company_id ON daily_summary (company_id);
CREATE INDEX idx_daily_summary_company_date ON daily_summary (company_id, summary_date);
