-- Стартовый справочник гаваней Таллинского залива и окрестностей.
-- Глубины/каналы/цены заполнить актуальными данными из lodbooks / сайтов гаваней.
insert into harbour (name, lat, lon, vhf_channel, depth_m, price_note)
values ('Pirita (Kalevi Jahtklubi)', 59.4672, 24.8303, null, null, null),
       ('Lennusadam (Tallinn)',      59.4519, 24.7383, null, null, null),
       ('Kakumäe (Haven Kakumäe)',   59.4472, 24.5836, null, null, null),
       ('Kelnase (Prangli)',         59.6386, 24.9925, null, null, null),
       ('Leppneeme (Viimsi)',        59.5581, 24.8686, null, null, null),
       ('Aegna',                     59.5769, 24.7469, null, null, null),
       ('Naissaare sadam',           59.5333, 24.5333, null, null, null);
