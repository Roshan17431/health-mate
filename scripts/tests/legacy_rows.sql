-- Deterministic synthetic records make whole-database preservation checks useful.
INSERT INTO public.users
    (id, name, email, password, phone_number, role, gender, date_of_birth, address, created_date, updated_date)
VALUES
    (1, 'Synthetic Doctor One', 'doctor1@example.invalid', 'fixture-placeholder', 'test-phone-1', 'ROLE_DOCTOR', 'MALE', '1980-01-01', 'Fixture clinic', '2020-01-01', '2020-01-02'),
    (2, 'Synthetic Doctor Two', 'doctor2@example.invalid', 'fixture-placeholder', 'test-phone-2', 'ROLE_DOCTOR', 'FEMALE', '1981-02-02', 'Fixture clinic', '2020-01-01', '2020-01-02'),
    (3, 'Synthetic Patient', 'patient@example.invalid', 'fixture-placeholder', 'test-phone-3', 'ROLE_PATIENT', 'FEMALE', '1990-03-03', 'Fixture address', '2020-01-01', '2020-01-02'),
    (4, 'Synthetic Administrator', 'admin@example.invalid', 'fixture-placeholder', 'test-phone-4', 'ROLE_ADMIN', 'MALE', '1982-04-04', 'Fixture clinic', '2020-01-01', '2020-01-02');
INSERT INTO public.doctors VALUES
    (1, 12, '09:00-17:00', 'Synthetic cardiology', '2020-01-01', '2020-01-02'),
    (2, 10, '09:00-17:00', 'Synthetic dermatology', '2020-01-01', '2020-01-02');
INSERT INTO public.patients VALUES (3, '2020-01-01', '2020-01-02');
INSERT INTO public.doctor_schedules VALUES
    (1, 1, 'WEDNESDAY', '09:00', '17:00', true, '2020-01-01', '2020-01-02');
INSERT INTO public.time_slots VALUES
    (1, 1, '2035-01-03', '09:00', '09:30', 'BOOKED', '2020-01-01', '2020-01-02', 1),
    (2, 1, '2035-01-03', '10:00', '10:30', 'AVAILABLE', '2020-01-01', '2020-01-02', 2),
    (3, 2, '2035-01-04', '11:00', '11:30', 'BOOKED', '2020-01-01', '2020-01-02', 3),
    (4, 1, '2035-01-03', '12:00', '12:30', 'AVAILABLE', '2020-01-01', '2020-01-02', 4);
INSERT INTO public.appointments VALUES
    (1, '2035-01-03 09:00', 'SCHEDULED', 'Synthetic history', 'FIXTURE-ACTIVE-1', '2020-01-01', '2020-01-02', 1, 3, 1),
    (2, '2035-01-03 10:00', 'CANCELED', 'Synthetic history', 'FIXTURE-CANCELED-2', '2020-01-01', '2020-01-02', 1, 3, 2),
    (3, '2035-01-04 11:00', 'COMPLETED', 'Synthetic history', 'FIXTURE-COMPLETED-3', '2020-01-01', '2020-01-02', 2, 3, 3),
    (4, '2035-01-05 14:00', 'CONFIRMED', 'Synthetic history', 'FIXTURE-SLOTLESS-4', '2020-01-01', '2020-01-02', 2, 3, NULL);
INSERT INTO public.notifications VALUES (1, 3, 'Synthetic appointment notice', false, '2020-01-02');
INSERT INTO public.otp_tokens VALUES
    (1, 'patient@example.invalid', '000000', 'PASSWORD_RESET', '2020-01-01', '2020-01-01 00:10', true, '2020-01-01 00:01');
INSERT INTO public.audit_logs VALUES
    (1, 4, 'SYNTHETIC_APPOINTMENT_CREATED', '{"appointmentCode":"FIXTURE-ACTIVE-1"}', '2020-01-01'),
    (2, NULL, 'SYNTHETIC_LEGACY_ANONYMOUS', 'Preserve unattributed history', '2020-01-02');
SELECT setval(pg_get_serial_sequence('public.users', 'id'), 4);
SELECT setval(pg_get_serial_sequence('public.time_slots', 'id'), 4);
SELECT setval(pg_get_serial_sequence('public.appointments', 'id'), 4);
SELECT setval(pg_get_serial_sequence('public.audit_logs', 'id'), 2);
SELECT setval(pg_get_serial_sequence('public.doctor_schedules', 'id'), 1);
SELECT setval(pg_get_serial_sequence('public.notifications', 'id'), 1);
SELECT setval(pg_get_serial_sequence('public.otp_tokens', 'id'), 1);
