-- Create User Table
CREATE TABLE IF NOT EXISTS t_user (
    id bigserial PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) DEFAULT 'USER',
    avatar BYTEA,
    -- Changed to BYTEA for binary storage
    signature VARCHAR(500),
    tags VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Insert Admin User (Optional)
-- INSERT INTO t_user (username, password, role, create_time, update_time) VALUES ('admin', 'admin', 'ADMIN', NOW(), NOW());
-- Create Game Table
CREATE TABLE IF NOT EXISTS t_game (
    id bigserial PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    cover BYTEA,
    description TEXT,
    tags VARCHAR(255),
    chatroom_link VARCHAR(255),
    lobby_link VARCHAR(255),
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Create Mute Table
CREATE TABLE IF NOT EXISTS t_mute (
    id bigserial PRIMARY KEY,
    user_id BIGINT NOT NULL,
    channel_id VARCHAR(50) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_time TIMESTAMP
);