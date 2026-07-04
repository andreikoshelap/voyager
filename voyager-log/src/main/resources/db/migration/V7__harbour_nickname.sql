alter table harbour add column nickname text;

-- Short Telegram-friendly names for bot input. Start with a readable
-- default derived from the official/user-facing harbour name, then override
-- ambiguous or overly long names below.
update harbour
set nickname = trim(
    regexp_replace(
        regexp_replace(
            regexp_replace(name, '\s*\(([^)]*)\)\s*$', '', 'i'),
            '\s+(guest\s+harbou?r|jahisadam|marina|sadam)$', '', 'i'
        ),
        '\s+', ' ', 'g'
    )
)
where nickname is null;

-- "Pirita" intentionally points to Kalevi Jahtklubi because this is the
-- default marina people usually mean in the bot flow; the registry also
-- contains Pirita TOP, addressable by EEPIR or "PiritaTop".
update harbour set nickname = 'Pirita' where harbour_code = 'EEKJK';
update harbour set nickname = 'PiritaTop' where harbour_code = 'EEPIR';

-- Keep the commonly used yacht-club abbreviations short.
update harbour set nickname = 'HSS' where name = 'HSS Guest Harbour (Liuskaluoto)';
update harbour set nickname = 'HMVK' where name = 'HMVK Guest Harbour (Tervasaari)';

-- Keep these more distinctive than the generic defaults.
update harbour set nickname = 'MarinaBay Helsinki' where name = 'MarinaBay Helsinki City Guest Harbour (Katajanokka)';
