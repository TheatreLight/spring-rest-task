-- Seed Hotels
INSERT INTO hotels (id, name, address) VALUES (1, 'Grand Plaza Hotel', '123 Main Street, New York, NY 10001');
INSERT INTO hotels (id, name, address) VALUES (2, 'Seaside Resort', '456 Ocean Drive, Miami, FL 33139');
INSERT INTO hotels (id, name, address) VALUES (3, 'Mountain Lodge', '789 Alpine Road, Denver, CO 80202');

-- Seed Rooms for Grand Plaza Hotel (ID: 1)
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (1, 1, '101', true, 5);
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (2, 1, '102', true, 3);
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (3, 1, '103', true, 7);
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (4, 1, '201', true, 2);
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (5, 1, '202', true, 4);
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (6, 1, '203', false, 0); -- Under maintenance

-- Seed Rooms for Seaside Resort (ID: 2)
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (7, 2, 'A1', true, 10);
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (8, 2, 'A2', true, 8);
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (9, 2, 'B1', true, 6);
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (10, 2, 'B2', true, 4);
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (11, 2, 'Penthouse', true, 15);

-- Seed Rooms for Mountain Lodge (ID: 3)
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (12, 3, 'Cabin-1', true, 1);
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (13, 3, 'Cabin-2', true, 2);
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (14, 3, 'Cabin-3', true, 0);
INSERT INTO rooms (id, hotel_id, number, available, times_booked) VALUES (15, 3, 'Suite-A', true, 3);
