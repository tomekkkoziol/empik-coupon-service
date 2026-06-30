CREATE TABLE coupons
(
    id              UUID         NOT NULL,
    code            VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    max_usages      INTEGER      NOT NULL,
    current_usages  INTEGER      NOT NULL DEFAULT 0,
    allowed_country CHAR(2)      NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_coupons PRIMARY KEY (id),
    CONSTRAINT uq_coupons_code UNIQUE (code),
    CONSTRAINT chk_max_usages_positive CHECK (max_usages > 0),
    CONSTRAINT chk_current_usages_non_negative CHECK (current_usages >= 0),
    CONSTRAINT chk_current_usages_within_limit CHECK (current_usages <= max_usages)
);

CREATE TABLE coupon_usages
(
    coupon_id UUID         NOT NULL,
    user_id   VARCHAR(255) NOT NULL,

    CONSTRAINT pk_coupon_usages PRIMARY KEY (coupon_id, user_id),
    CONSTRAINT fk_coupon_usages_coupon
        FOREIGN KEY (coupon_id)
            REFERENCES coupons (id)
            ON DELETE CASCADE
);
