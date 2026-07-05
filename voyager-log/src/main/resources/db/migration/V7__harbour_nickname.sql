alter table harbour add column nickname text;

-- Short Telegram-friendly name. "Pirita" intentionally points to Kalevi
-- Jahtklubi because this is the default marina people usually mean in the
-- bot flow; the registry also contains "Pirita sadam", addressable by code
-- EEPIR or its full name.
update harbour
set nickname = 'Pirita'
where harbour_code = 'EEKJK';
