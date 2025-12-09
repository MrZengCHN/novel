-- Create User Table
CREATE TABLE IF NOT EXISTS t_user (
    id bigserial PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) DEFAULT 'USER',
    avatar VARCHAR(255),
    signature VARCHAR(500),
    tags VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Insert Admin User (Optional)
-- INSERT INTO t_user (username, password, role, create_time, update_time) VALUES ('admin', 'admin', 'ADMIN', NOW(), NOW());