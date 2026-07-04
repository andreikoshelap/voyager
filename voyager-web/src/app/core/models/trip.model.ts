export type TripStatus = 'PLANNED' | 'AT_SEA' | 'COMPLETED' | 'OVERDUE' | 'ALERTED';
export type LocationConfidence = 'CONFIRMED' | 'APPROXIMATE';

/**
 * UPDATED: matches ee.voyagelog.trip.ActiveTripResponse after core patch 4.
 * markerLat/markerLon are precomputed on the backend — CONFIRMED means the
 * destination text matched a real harbour and the marker sits on it;
 * APPROXIMATE means it's a computed point ~1 nm off the departure harbour,
 * never a claimed live position (no GPS is collected). Either field can be
 * null for a handful of legacy test trips created before this migration —
 * the map component skips drawing a marker in that case.
 */
export interface ActiveTrip {
  id: number;
  departureHarbourId: number | null;
  destinationHarbourId: number | null;
  destination: string;
  markerLat: number | null;
  markerLon: number | null;
  locationConfidence: LocationConfidence | null;
  status: TripStatus;
  crewCount: number;
  departedAt: string;
  etaReturn: string;
}
