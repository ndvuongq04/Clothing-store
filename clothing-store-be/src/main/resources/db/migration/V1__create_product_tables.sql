-- =============================================
-- V1: Product module tables
-- =============================================

-- 1. categories (self-referencing tree)
CREATE TABLE IF NOT EXISTS categories (
    id            VARCHAR(36)   PRIMARY KEY,
    parent_id     VARCHAR(36)   NULL,
    name          VARCHAR(100)  NOT NULL,
    CONSTRAINT fk_cat_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

-- 2. products
CREATE TABLE IF NOT EXISTS products (
    id            VARCHAR(36)   PRIMARY KEY,
    category_id   VARCHAR(36)   NOT NULL,
    name          VARCHAR(200)  NOT NULL,
    description   TEXT          NULL,
    base_price    DECIMAL(15,0) NOT NULL,
    thumbnail_url VARCHAR(500)  NULL,
    status        TINYINT(1)    NOT NULL DEFAULT 1,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    DATETIME      NULL,
    CONSTRAINT fk_prod_cat FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- 3. product_variants
CREATE TABLE IF NOT EXISTS product_variants (
    id            VARCHAR(36)   PRIMARY KEY,
    product_id    VARCHAR(36)   NOT NULL,
    color         VARCHAR(50)   NULL,
    size          VARCHAR(10)   NULL,
    stock_qty     INT           NOT NULL DEFAULT 0,
    sale_price    DECIMAL(15,0) NULL,
    sku           VARCHAR(100)  NULL,
    CONSTRAINT fk_var_prod FOREIGN KEY (product_id) REFERENCES products(id)
);

-- 4. product_images
CREATE TABLE IF NOT EXISTS product_images (
    id            VARCHAR(36)   PRIMARY KEY,
    product_id    VARCHAR(36)   NOT NULL,
    image_url     VARCHAR(500)  NOT NULL,
    sort_order    INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_img_prod FOREIGN KEY (product_id) REFERENCES products(id)
);
