CREATE TABLE reviews (
    id          BIGSERIAL PRIMARY KEY,
    lawyer_id   BIGINT NOT NULL,
    client_id   BIGINT NOT NULL,
    booking_id  BIGINT NOT NULL,
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     VARCHAR(1000),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_reviews_booking_id ON reviews(booking_id);
CREATE INDEX idx_reviews_lawyer_id ON reviews(lawyer_id);