CREATE TABLE product
(
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(32)    NOT NULL UNIQUE,
    name        VARCHAR(120)   NOT NULL,
    description VARCHAR(1000),
    category    VARCHAR(60)    NOT NULL,
    price       NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    stock       INTEGER        NOT NULL CHECK (stock >= 0),
    version     INTEGER        NOT NULL DEFAULT 0
);

CREATE TABLE purchase_order
(
    id                 BIGSERIAL PRIMARY KEY,
    customer_reference VARCHAR(64)    NOT NULL,
    status             VARCHAR(20)    NOT NULL,
    total              NUMERIC(14, 2) NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    version            INTEGER        NOT NULL DEFAULT 0
);

CREATE TABLE order_line
(
    order_id   BIGINT         NOT NULL REFERENCES purchase_order (id) ON DELETE CASCADE,
    line_no    INTEGER        NOT NULL,
    sku        VARCHAR(32)    NOT NULL,
    quantity   INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12, 2) NOT NULL,
    PRIMARY KEY (order_id, line_no)
);

CREATE INDEX idx_product_category ON product (category);
CREATE INDEX idx_purchase_order_customer ON purchase_order (customer_reference);
