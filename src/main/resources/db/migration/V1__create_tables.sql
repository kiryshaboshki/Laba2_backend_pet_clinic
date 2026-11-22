CREATE TABLE pets (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    type          VARCHAR(50)  NOT NULL,
    owner_name    VARCHAR(255) NOT NULL,
    vaccinated    BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE appointments (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pet_id           UUID        NOT NULL REFERENCES pets(id),
    vet_name         VARCHAR(255) NOT NULL,
    date_time        TIMESTAMP    NOT NULL,
    duration_minutes INTEGER      NOT NULL,
    status           VARCHAR(50)  NOT NULL
);
