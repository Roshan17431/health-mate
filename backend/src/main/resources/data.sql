INSERT INTO users (
    name,
    email,
    password,
    phone_number,
    role,
    gender,
    date_of_birth,
    address,
    created_date,
    updated_date
) VALUES (
    'System Administrator',
    'admin@healthmate.com',
    '$2a$10$QyIsgV3nmQW80niXrGKsHuWLCOChidoBvxuXX5YVuQJtdFRIoW6xe',
    '+10000000000',
    'ROLE_ADMIN',
    'OTHER',
    '1990-01-01',
    'Health Mate Administration',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;
