-- Seed Users
-- Password for all users is 'password123' (BCrypt encoded)
-- $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n93bFXJxhfgJQ8nXKDfgi

INSERT INTO users (id, username, password, role, created_at) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n93bFXJxhfgJQ8nXKDfgi', 'ADMIN', CURRENT_TIMESTAMP),
(2, 'john_doe', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n93bFXJxhfgJQ8nXKDfgi', 'USER', CURRENT_TIMESTAMP),
(3, 'jane_smith', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n93bFXJxhfgJQ8nXKDfgi', 'USER', CURRENT_TIMESTAMP),
(4, 'bob_wilson', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n93bFXJxhfgJQ8nXKDfgi', 'USER', CURRENT_TIMESTAMP);

-- Seed some sample bookings (room IDs correspond to rooms in hotel-service)
INSERT INTO bookings (id, user_id, room_id, hotel_id, start_date, end_date, status, request_id, created_at) VALUES
(1, 2, 1, 1, DATEADD('DAY', 7, CURRENT_DATE), DATEADD('DAY', 10, CURRENT_DATE), 'CONFIRMED', 'req-001', CURRENT_TIMESTAMP),
(2, 2, 7, 2, DATEADD('DAY', 14, CURRENT_DATE), DATEADD('DAY', 17, CURRENT_DATE), 'CONFIRMED', 'req-002', CURRENT_TIMESTAMP),
(3, 3, 12, 3, DATEADD('DAY', 5, CURRENT_DATE), DATEADD('DAY', 8, CURRENT_DATE), 'CONFIRMED', 'req-003', CURRENT_TIMESTAMP),
(4, 4, 2, 1, DATEADD('DAY', 20, CURRENT_DATE), DATEADD('DAY', 22, CURRENT_DATE), 'PENDING', 'req-004', CURRENT_TIMESTAMP);
