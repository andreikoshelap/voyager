-- Adds fields needed to place an honest map marker for an active trip:
--   destination_harbour_id  -- set only when the typed destination matched
--                               a real harbour (CONFIRMED)
--   marker_lat / marker_lon -- always populated for new trips: either the
--                               matched harbour's coordinates, or a point
--                               ~1 nautical mile off the departure harbour
--                               (APPROXIMATE) — see GeoUtil.destinationPoint
--   location_confidence     -- CONFIRMED | APPROXIMATE, drives marker style
--
-- Nullable by design: existing rows from earlier testing simply have no
-- marker (the frontend skips drawing when markerLat/markerLon are null)
-- rather than forcing a backfill guess.
alter table trip add column destination_harbour_id bigint references harbour (id);
alter table trip add column marker_lat double precision;
alter table trip add column marker_lon double precision;
alter table trip add column location_confidence varchar(20);

-- Rough manual bearing (degrees) toward open water, used as the fallback
-- direction for the "somewhere out here" marker when no destination is
-- given or resolved. These are eyeballed from the harbour coordinates in
-- V2, not surveyed — good enough to avoid dropping a marker on land for
-- a demo app, not a substitute for real chart data.
alter table harbour add column seaward_bearing_deg integer;

update harbour set seaward_bearing_deg = 10  where name like 'Pirita%';
update harbour set seaward_bearing_deg = 0   where name like 'Lennusadam%';
update harbour set seaward_bearing_deg = 270 where name like 'Kakumäe%';
update harbour set seaward_bearing_deg = 200 where name like 'Kelnase%';
update harbour set seaward_bearing_deg = 90  where name like 'Leppneeme%';
update harbour set seaward_bearing_deg = 0   where name like 'Aegna%';
update harbour set seaward_bearing_deg = 270 where name like 'Naissaar%';
