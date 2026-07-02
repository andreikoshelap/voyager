export type TripStatus = 'PLANNED' | 'AT_SEA' | 'COMPLETED' | 'OVERDUE' | 'ALERTED';

/** Matches ee.voyagelog.trip.ActiveTripResponse — no personal data. */
export interface ActiveTrip {
  id: number;
  destination: string;
  status: TripStatus;
  crewCount: number;
  departedAt: string;
  etaReturn: string;
}
