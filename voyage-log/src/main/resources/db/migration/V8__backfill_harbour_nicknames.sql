-- Backfill for databases where the first nickname migration had already
-- run with only the Pirita/KJK row populated.
update harbour
set nickname = trim(
    regexp_replace(
        regexp_replace(
            regexp_replace(name, '\s*\(([^)]*)\)\s*$', '', 'i'),
            '\s+(guest\s+harbou?r|jahisadam|marina|sadam)$', '', 'i'
        ),
        '\s+', ' ', 'g'
    )
);

update harbour set nickname = 'Pirita' where harbour_code = 'EEKJK';
update harbour set nickname = 'PiritaTop' where harbour_code = 'EEPIR';
update harbour set nickname = 'HSS' where name = 'HSS Guest Harbour (Liuskaluoto)';
update harbour set nickname = 'HMVK' where name = 'HMVK Guest Harbour (Tervasaari)';
update harbour set nickname = 'MarinaBay Helsinki' where name = 'MarinaBay Helsinki City Guest Harbour (Katajanokka)';
