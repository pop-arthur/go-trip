CREATE TABLE IF NOT EXISTS user_notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_id    BIGINT REFERENCES orders(id) ON DELETE SET NULL,
    type        TEXT NOT NULL CHECK (type IN ('STATUS_CHANGE', 'REMINDER', 'GENERAL', 'PROMO', 'OTHER')),
    title       TEXT NOT NULL,
    body        TEXT NOT NULL,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_notifications_user_id ON user_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_user_notifications_order_id ON user_notifications(order_id);
CREATE INDEX IF NOT EXISTS idx_user_notifications_sent_at ON user_notifications(sent_at);
CREATE INDEX IF NOT EXISTS idx_user_notifications_is_read ON user_notifications(is_read);