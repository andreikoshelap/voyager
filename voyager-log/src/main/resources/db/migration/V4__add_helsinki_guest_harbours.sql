insert into harbour (name, lat, lon, vhf_channel, depth_m, price_note)
select name, lat, lon, vhf_channel, depth_m, price_note
from (
    values
        ('MarinaBay Helsinki City Guest Harbour (Katajanokka)', 60.1692, 24.9658, '68', 5.0, 'Guest berths, about 45-60 EUR/night in high season.'),
        ('HSS Guest Harbour (Liuskaluoto)', 60.1528, 24.9470, '68', 5.0, 'Guest marina of Helsingfors Segelsallskap yacht club, about 35-50 EUR/night.'),
        ('HMVK Guest Harbour (Tervasaari)', 60.1719, 24.9595, '68', 3.5, 'About 30-45 EUR/night.'),
        ('Vallisaari Guest Harbor', 60.1476, 25.0005, '68', 3.0, 'Seasonal guest harbour, about 25-40 EUR/night.'),
        ('Otsolahti Marina (Espoo)', 60.1816, 24.8238, null, 3.0, 'Guest berths subject to availability, about 25-40 EUR/night.')
) as new_harbours(name, lat, lon, vhf_channel, depth_m, price_note)
where not exists (
    select 1
    from harbour h
    where h.name = new_harbours.name
);
