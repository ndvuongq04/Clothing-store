-- =============================================
-- V2: Cart module tables
-- =============================================

-- 1. vouchers
CREATE TABLE IF NOT EXISTS vouchers (
    voucher_code      VARCHAR(50)   PRIMARY KEY,
    type              VARCHAR(10)   NOT NULL COMMENT 'percent | fixed',
    discount_value    DECIMAL(15,0) NOT NULL,
    min_order_value   DECIMAL(15,0) NOT NULL DEFAULT 0,
    max_discount_cap  DECIMAL(15,0) NULL,
    start_date        DATE          NOT NULL,
    expiry_date       DATE          NOT NULL,
    max_usage         INT           NOT NULL,
    used_count        INT           NOT NULL DEFAULT 0,
    active            TINYINT(1)    NOT NULL DEFAULT 1,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. carts (1 user = 1 cart)
CREATE TABLE IF NOT EXISTS carts (
    id               BIGINT        PRIMARY KEY AUTO_INCREMENT,
    user_id          BIGINT        NOT NULL UNIQUE,
    voucher_code     VARCHAR(50)   NULL,
    sub_total        DECIMAL(15,0) NOT NULL DEFAULT 0,
    discount_amount  DECIMAL(15,0) NOT NULL DEFAULT 0,
    total            DECIMAL(15,0) NOT NULL DEFAULT 0,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_user    FOREIGN KEY (user_id)      REFERENCES users(user_id),
    CONSTRAINT fk_cart_voucher FOREIGN KEY (voucher_code) REFERENCES vouchers(voucher_code)
);

-- 3. cart_items
CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT,
    cart_id     BIGINT        NOT NULL,
    variant_id  VARCHAR(36)   NOT NULL,
    quantity    INT           NOT NULL DEFAULT 1,
    unit_price  DECIMAL(15,0) NOT NULL,
    CONSTRAINT fk_ci_cart      FOREIGN KEY (cart_id)    REFERENCES carts(id),
    CONSTRAINT fk_ci_variant   FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    CONSTRAINT uq_cart_variant UNIQUE (cart_id, variant_id)
);
