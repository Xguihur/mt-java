-- 建表脚本。
-- 使用 IF NOT EXISTS，每次启动都执行但不会报错，也不会清空已有数据。

CREATE TABLE IF NOT EXISTS users
(
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    created_at DATETIME     NOT NULL,
    UNIQUE KEY uk_phone (phone)
);

CREATE TABLE IF NOT EXISTS red_packet
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title               VARCHAR(200) NOT NULL,
    total_amount_cents  INT          NOT NULL COMMENT '总金额（分）',
    total_count         INT          NOT NULL COMMENT '总个数',
    remain_amount_cents INT          NOT NULL COMMENT '剩余金额（分）',
    remain_count        INT          NOT NULL COMMENT '剩余个数',
    created_at          DATETIME     NOT NULL
);

CREATE TABLE IF NOT EXISTS red_packet_record
(
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    red_packet_id  BIGINT       NOT NULL,
    user_id        BIGINT       NOT NULL,
    user_name      VARCHAR(100) NOT NULL,
    amount_cents   INT          NOT NULL COMMENT '抢到的金额（分）',
    grabbed_at     DATETIME     NOT NULL,
    KEY idx_red_packet_id (red_packet_id)
);
