CREATE TABLE route
(
    id             UUID PRIMARY KEY,
    origin         VARCHAR(3)               NOT NULL CHECK ( origin = UPPER(origin) ),
    destination    VARCHAR(3)               NOT NULL CHECK ( destination = UPPER(destination)),
    departure_date DATE                     NOT NULL,
    active         BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (origin, destination, departure_date)
);

CREATE TABLE price_snapshot
(
    id           UUID PRIMARY KEY,
    route_id     UUID                     NOT NULL,
    price        NUMERIC(10, 2)           NOT NULL CHECK ( price > 0 ),
    currency     VARCHAR(3)               NOT NULL CHECK ( currency = UPPER(currency) ),
    retrieved_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_anomaly   BOOLEAN                  NOT NULL DEFAULT FALSE,
    FOREIGN KEY (route_id) REFERENCES route (id) ON DELETE CASCADE
);

CREATE INDEX idx_price_snapshot_route_id ON price_snapshot (route_id);
CREATE INDEX idx_price_snapshot_retrieved_at ON price_snapshot (retrieved_at);
CREATE INDEX idx_price_snapshot_anomalies ON price_snapshot (is_anomaly) WHERE is_anomaly IS TRUE;