import { inject } from '@angular/core';
import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { interval, pipe, startWith, switchMap, tap } from 'rxjs';
import { TripService } from '../core/api/trip.service';
import { ActiveTrip } from '../core/models/trip.model';

interface TripState {
  activeTrips: ActiveTrip[];
  loading: boolean;
}

const POLL_INTERVAL_MS = 5_000;

/**
 * Polls /api/trips/active every 30s to drive the "boats at sea" layer.
 * A later pass can swap this rxMethod body for an SSE stream without
 * touching the map component that reads `activeTrips`.
 */
export const TripStore = signalStore(
  { providedIn: 'root' },
  withState<TripState>({ activeTrips: [], loading: false }),
  withMethods((store, tripService = inject(TripService)) => ({
    refresh: rxMethod<void>(
      pipe(
        tap(() => patchState(store, { loading: true })),
        switchMap(() =>
          tripService.getActive().pipe(
            tapResponse({
              next: (activeTrips) => patchState(store, { activeTrips, loading: false }),
              error: () => patchState(store, { loading: false }),
            }),
          ),
        ),
      ),
    ),
    poll: rxMethod<void>(
      pipe(
        switchMap(() =>
          interval(POLL_INTERVAL_MS).pipe(
            startWith(0),
            tap(() => patchState(store, { loading: true })),
            switchMap(() =>
              tripService.getActive().pipe(
                tapResponse({
                  next: (activeTrips) => patchState(store, { activeTrips, loading: false }),
                  error: () => patchState(store, { loading: false }),
                }),
              ),
            ),
          ),
        ),
      ),
    ),
  })),
  withHooks({
    onInit(store) {
      store.poll(undefined);
    },
  }),
);
