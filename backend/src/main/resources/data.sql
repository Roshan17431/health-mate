-- Production bootstrap data for Health Mate.
-- Initial password for seeded users is: admin123

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
) VALUES
    (
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
    ),
    (
        'Dr. Emily Chen',
        'emily.chen@healthmate.com',
        '$2a$10$QyIsgV3nmQW80niXrGKsHuWLCOChidoBvxuXX5YVuQJtdFRIoW6xe',
        '+15551000001',
        'ROLE_DOCTOR',
        'FEMALE',
        '1985-05-12',
        '123 Medical Plaza',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Dr. James Wilson',
        'james.wilson@healthmate.com',
        '$2a$10$QyIsgV3nmQW80niXrGKsHuWLCOChidoBvxuXX5YVuQJtdFRIoW6xe',
        '+15551000002',
        'ROLE_DOCTOR',
        'MALE',
        '1978-08-22',
        '456 Health Way',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Dr. Priya Nair',
        'priya.nair@healthmate.com',
        '$2a$10$QyIsgV3nmQW80niXrGKsHuWLCOChidoBvxuXX5YVuQJtdFRIoW6xe',
        '+15551000003',
        'ROLE_DOCTOR',
        'FEMALE',
        '1982-02-18',
        '789 Care Center',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Alice Smith',
        'alice.smith@example.com',
        '$2a$10$QyIsgV3nmQW80niXrGKsHuWLCOChidoBvxuXX5YVuQJtdFRIoW6xe',
        '+15552000001',
        'ROLE_PATIENT',
        'FEMALE',
        '1992-11-05',
        '789 Oak Street',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Bob Johnson',
        'bob.johnson@example.com',
        '$2a$10$QyIsgV3nmQW80niXrGKsHuWLCOChidoBvxuXX5YVuQJtdFRIoW6xe',
        '+15552000002',
        'ROLE_PATIENT',
        'MALE',
        '1989-03-15',
        '321 Pine Avenue',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'Maya Patel',
        'maya.patel@example.com',
        '$2a$10$QyIsgV3nmQW80niXrGKsHuWLCOChidoBvxuXX5YVuQJtdFRIoW6xe',
        '+15552000003',
        'ROLE_PATIENT',
        'FEMALE',
        '1994-07-21',
        '42 Maple Clinic Road',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (email) DO UPDATE SET
    name = EXCLUDED.name,
    phone_number = EXCLUDED.phone_number,
    role = EXCLUDED.role,
    gender = EXCLUDED.gender,
    date_of_birth = EXCLUDED.date_of_birth,
    address = EXCLUDED.address,
    updated_date = CURRENT_TIMESTAMP;

INSERT INTO doctors (user_id, experience, available_hours, specialization, created_at, updated_at)
SELECT u.id, 10, '09:00-17:00', 'Cardiology', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'emily.chen@healthmate.com'
ON CONFLICT (user_id) DO UPDATE SET
    experience = EXCLUDED.experience,
    available_hours = EXCLUDED.available_hours,
    specialization = EXCLUDED.specialization,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO doctors (user_id, experience, available_hours, specialization, created_at, updated_at)
SELECT u.id, 15, '10:00-18:00', 'General Practice', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'james.wilson@healthmate.com'
ON CONFLICT (user_id) DO UPDATE SET
    experience = EXCLUDED.experience,
    available_hours = EXCLUDED.available_hours,
    specialization = EXCLUDED.specialization,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO doctors (user_id, experience, available_hours, specialization, created_at, updated_at)
SELECT u.id, 8, '08:30-16:30', 'Dermatology', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'priya.nair@healthmate.com'
ON CONFLICT (user_id) DO UPDATE SET
    experience = EXCLUDED.experience,
    available_hours = EXCLUDED.available_hours,
    specialization = EXCLUDED.specialization,
    updated_at = CURRENT_TIMESTAMP;

-- Ensure every patient user, including pre-existing accounts, has a patient profile.
INSERT INTO patients (user_id, created_at, updated_at)
SELECT u.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE u.role = 'ROLE_PATIENT'
  AND NOT EXISTS (
      SELECT 1
      FROM patients p
      WHERE p.user_id = u.id
  );

INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time, is_active, created_at, updated_at)
SELECT d.user_id, 'MONDAY', '09:00:00', '17:00:00', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE u.email = 'emily.chen@healthmate.com'
  AND NOT EXISTS (
      SELECT 1 FROM doctor_schedules s WHERE s.doctor_id = d.user_id AND s.day_of_week = 'MONDAY'
  );

INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time, is_active, created_at, updated_at)
SELECT d.user_id, 'TUESDAY', '09:00:00', '17:00:00', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE u.email = 'emily.chen@healthmate.com'
  AND NOT EXISTS (
      SELECT 1 FROM doctor_schedules s WHERE s.doctor_id = d.user_id AND s.day_of_week = 'TUESDAY'
  );

INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time, is_active, created_at, updated_at)
SELECT d.user_id, 'WEDNESDAY', '10:00:00', '18:00:00', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE u.email = 'james.wilson@healthmate.com'
  AND NOT EXISTS (
      SELECT 1 FROM doctor_schedules s WHERE s.doctor_id = d.user_id AND s.day_of_week = 'WEDNESDAY'
  );

INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time, is_active, created_at, updated_at)
SELECT d.user_id, 'THURSDAY', '10:00:00', '18:00:00', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE u.email = 'james.wilson@healthmate.com'
  AND NOT EXISTS (
      SELECT 1 FROM doctor_schedules s WHERE s.doctor_id = d.user_id AND s.day_of_week = 'THURSDAY'
  );

INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time, is_active, created_at, updated_at)
SELECT d.user_id, 'FRIDAY', '08:30:00', '16:30:00', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctors d
JOIN users u ON u.id = d.user_id
WHERE u.email = 'priya.nair@healthmate.com'
  AND NOT EXISTS (
      SELECT 1 FROM doctor_schedules s WHERE s.doctor_id = d.user_id AND s.day_of_week = 'FRIDAY'
  );

INSERT INTO time_slots (doctor_id, slot_date, start_time, end_time, status, version, created_at, updated_at)
SELECT d.user_id, slot_template.slot_date, slot_template.start_time, slot_template.end_time, 'AVAILABLE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctors d
JOIN users u ON u.id = d.user_id
CROSS JOIN (
    VALUES
        (CURRENT_DATE + 1, TIME '09:00:00', TIME '09:30:00'),
        (CURRENT_DATE + 1, TIME '09:30:00', TIME '10:00:00'),
        (CURRENT_DATE + 1, TIME '10:00:00', TIME '10:30:00'),
        (CURRENT_DATE + 2, TIME '14:00:00', TIME '14:30:00'),
        (CURRENT_DATE + 2, TIME '14:30:00', TIME '15:00:00'),
        (CURRENT_DATE + 3, TIME '11:00:00', TIME '11:30:00')
) AS slot_template(slot_date, start_time, end_time)
WHERE u.email IN (
    'emily.chen@healthmate.com',
    'james.wilson@healthmate.com',
    'priya.nair@healthmate.com'
)
ON CONFLICT (doctor_id, slot_date, start_time) DO NOTHING;

-- One realistic existing appointment while preserving many available slots.
INSERT INTO appointments (
    appointment_date_time,
    status,
    notes,
    appointment_code,
    created_at,
    updated_at,
    doctor_id,
    patient_id,
    time_slot_id
)
SELECT
    (CURRENT_DATE + 3) + TIME '11:00:00',
    'CONFIRMED',
    'Initial cardiology consultation for recurring fatigue.',
    'APT-SEED-001',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    d.user_id,
    p.user_id,
    ts.id
FROM doctors d
JOIN users doctor_user ON doctor_user.id = d.user_id AND doctor_user.email = 'emily.chen@healthmate.com'
JOIN users patient_user ON patient_user.email = 'alice.smith@example.com'
JOIN patients p ON p.user_id = patient_user.id
JOIN time_slots ts
  ON ts.doctor_id = d.user_id
 AND ts.slot_date = CURRENT_DATE + 3
 AND ts.start_time = TIME '11:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM appointments a
    WHERE a.appointment_code = 'APT-SEED-001'
)
  AND NOT EXISTS (
      SELECT 1
      FROM appointments a
      WHERE a.time_slot_id = ts.id
  );

UPDATE time_slots
SET status = 'BOOKED', updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    SELECT time_slot_id
    FROM appointments
    WHERE appointment_code = 'APT-SEED-001'
);

SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1), true);
SELECT setval(pg_get_serial_sequence('doctor_schedules', 'id'), COALESCE((SELECT MAX(id) FROM doctor_schedules), 1), true);
SELECT setval(pg_get_serial_sequence('time_slots', 'id'), COALESCE((SELECT MAX(id) FROM time_slots), 1), true);
SELECT setval(pg_get_serial_sequence('appointments', 'id'), COALESCE((SELECT MAX(id) FROM appointments), 1), true);
