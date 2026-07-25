-- EuchreFlow initial schema
-- Multi-tenant: every tenant-scoped row carries group_id.

CREATE TABLE groups (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug        VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    theme       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE players (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_id    BIGINT       NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    email       VARCHAR(320),
    join_code   VARCHAR(16)  NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_players_group ON players(group_id);

CREATE TABLE tournaments (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_id       BIGINT       NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    name           VARCHAR(200) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    scheduler_id   VARCHAR(64)  NOT NULL DEFAULT 'standard-card',
    num_rounds     INT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_tournaments_group ON tournaments(group_id);

-- Which players are enrolled in a given tournament, with a stable seat index.
CREATE TABLE enrollments (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tournament_id  BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    player_id      BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    seat_index     INT    NOT NULL,
    UNIQUE (tournament_id, player_id),
    UNIQUE (tournament_id, seat_index)
);

CREATE TABLE rounds (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tournament_id  BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    round_number   INT    NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    UNIQUE (tournament_id, round_number)
);

-- One game played at one table during one round: two partnerships.
CREATE TABLE seatings (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    round_id       BIGINT NOT NULL REFERENCES rounds(id) ON DELETE CASCADE,
    table_number   INT    NOT NULL,
    team1_p1       BIGINT NOT NULL REFERENCES players(id),
    team1_p2       BIGINT NOT NULL REFERENCES players(id),
    team2_p1       BIGINT NOT NULL REFERENCES players(id),
    team2_p2       BIGINT NOT NULL REFERENCES players(id),
    UNIQUE (round_id, table_number)
);
CREATE INDEX idx_seatings_round ON seatings(round_id);

-- The recorded result for a seating. One row per seating (upsert on re-entry).
CREATE TABLE game_scores (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    seating_id         BIGINT NOT NULL UNIQUE REFERENCES seatings(id) ON DELETE CASCADE,
    team1_score        INT    NOT NULL,
    team2_score        INT    NOT NULL,
    submitted_by       BIGINT REFERENCES players(id),
    submitted_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE organizers (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    group_id     BIGINT       NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    email        VARCHAR(320) NOT NULL,
    display_name VARCHAR(200),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (group_id, email)
);
