-- =============================================
-- V4: Address + Order module tables
-- =============================================

-- 1. addresses
CREATE TABLE IF NOT EXISTS addresses (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL,
    full_name   VARCHAR(100)  NOT NULL,
    phone       VARCHAR(15)   NOT NULL,
    province    VARCHAR(100)  NOT NULL,
    district    VARCHAR(100)  NOT NULL,
    ward        VARCHAR(100)  NOT NULL,
    street      VARCHAR(255)  NOT NULL,
    is_default  TINYINT(1)    NOT NULL DEFAULT 0,
    deleted_at  DATETIME      NULL,
    CONSTRAINT fk_addr_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 2. orders
CREATE TABLE IF NOT EXISTS orders (
    id               BIGINT        PRIMARY KEY AUTO_INCREMENT,
    order_code       VARCHAR(30)   NOT NULL UNIQUE,
    user_id          BIGINT        NOT NULL,
    address_id       BIGINT        NOT NULL,
    voucher_code     VARCHAR(50)   NULL,
    status           VARCHAR(30)   NOT NULL DEFAULT 'pending',
    payment_method   VARCHAR(20)   NULL,
    payment_status   VARCHAR(20)   NOT NULL DEFAULT 'unpaid',
    sub_total        DECIMAL(15,0) NOT NULL,
    discount_amount  DECIMAL(15,0) NOT NULL DEFAULT 0,
    total            DECIMAL(15,0) NOT NULL,
    note             TEXT          NULL,
    tracking_code    VARCHAR(100)  NULL,
    cancel_reason    TEXT          NULL,
    cancelled_by     VARCHAR(20)   NULL,
    cancelled_at     DATETIME      NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user    FOREIGN KEY (user_id)      REFERENCES users(user_id),
    CONSTRAINT fk_order_addr    FOREIGN KEY (address_id)   REFERENCES addresses(id),
    CONSTRAINT fk_order_voucher FOREIGN KEY (voucher_code) REFERENCES vouchers(voucher_code)
);

-- 3. order_items
CREATE TABLE IF NOT EXISTS order_items (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    order_id      BIGINT        NOT NULL,
    variant_id    VARCHAR(36)   NOT NULL,
    product_name  VARCHAR(200)  NOT NULL,
    color         VARCHAR(50)   NULL,
    size          VARCHAR(10)   NULL,
    thumbnail_url VARCHAR(500)  NULL,
    quantity      INT           NOT NULL,
    unit_price    DECIMAL(15,0) NOT NULL,
    line_total    DECIMAL(15,0) NOT NULL,
    CONSTRAINT fk_oi_order   FOREIGN KEY (order_id)   REFERENCES orders(id),
    CONSTRAINT fk_oi_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);

-- 4. payments (1-to-1 with orders)
CREATE TABLE IF NOT EXISTS payments (
    id                    BIGINT        PRIMARY KEY AUTO_INCREMENT,
    order_id              BIGINT        NOT NULL UNIQUE,
    method                VARCHAR(20)   NOT NULL,
    amount                DECIMAL(15,0) NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'pending',
    vnpay_txn_ref         VARCHAR(100)  NULL,
    vnpay_transaction_no  VARCHAR(100)  NULL,
    vnpay_response_code   VARCHAR(10)   NULL,
    vnpay_pay_date        VARCHAR(20)   NULL,
    paid_at               DATETIME      NULL,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pay_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- 5. payment_settings
CREATE TABLE IF NOT EXISTS payment_settings (
    method      VARCHAR(20)  PRIMARY KEY,
    is_enabled  TINYINT(1)   NOT NULL DEFAULT 1,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
INSERT INTO payment_settings (method, is_enabled) VALUES ('cod', 1), ('vnpay', 1);
