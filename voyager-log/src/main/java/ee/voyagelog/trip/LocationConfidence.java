package ee.voyagelog.trip;

/**
 * CONFIRMED: destination text matched a real harbour, marker sits exactly
 * on it. APPROXIMATE: no match (or no destination given) — marker is a
 * computed point ~1 nm off the departure harbour, meant to read as
 * "somewhere out here", never as a precise position.
 */
public enum LocationConfidence {
    CONFIRMED,
    APPROXIMATE
}
