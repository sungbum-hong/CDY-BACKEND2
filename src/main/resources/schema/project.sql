CREATE TABLE IF NOT EXISTS project (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    title           VARCHAR(255) NOT NULL,
    slogan          VARCHAR(255),
    description     LONGTEXT,
    image_url       VARCHAR(500),
    max_participants INT,
    deadline        VARCHAR(50),
    question        TEXT,
    open_talk_url   VARCHAR(500),
    likes           INT DEFAULT 0,
    is_deleted      BOOLEAN DEFAULT FALSE,
    created_at      DATETIME,
    updated_at      DATETIME
);

CREATE TABLE IF NOT EXISTS project_skill (
    project_id BIGINT NOT NULL,
    skill      VARCHAR(100) NOT NULL,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS project_position (
    project_id      BIGINT NOT NULL,
    position_name   VARCHAR(100) NOT NULL,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
);
