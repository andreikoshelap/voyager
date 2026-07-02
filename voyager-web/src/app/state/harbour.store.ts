import { computed, inject } from '@angular/core';
import { tapResponse } from '@ngrx/operators';
import {
  patchState,
  signalStore,
  withComputed,
  withMethods,
  withState,
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, switchMap, tap } from 'rxjs';
import { HarbourService } from '../core/api/harbour.service';
import { Harbour } from '../core/models/harbour.model';

interface HarbourState {
  harbours: Harbour[];
  selectedId: number | null;
  loading: boolean;
  error: string | null;
}

const initialState: HarbourState = {
  harbours: [],
  selectedId: null,
  loading: false,
  error: null,
};

/**
 * Single source of truth for the harbour list and the current map selection.
 * The chart map and the info panel both read `selected` — a marker click
 * just calls `select(id)`, no @Output plumbing between them.
 */
export const HarbourStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ harbours, selectedId }) => ({
    selected: computed(() => harbours().find((h) => h.id === selectedId()) ?? null),
    hostedCount: computed(() => harbours().filter((h) => h.hasHost).length),
  })),
  withMethods((store, harbourService = inject(HarbourService)) => ({
    select(id: number): void {
      patchState(store, { selectedId: id });
    },
    load: rxMethod<void>(
      pipe(
        tap(() => patchState(store, { loading: true, error: null })),
        switchMap(() =>
          harbourService.getAll().pipe(
            tapResponse({
              next: (harbours) =>
                patchState(store, {
                  harbours,
                  loading: false,
                  selectedId: store.selectedId() ?? harbours[0]?.id ?? null,
                }),
              error: () =>
                patchState(store, { loading: false, error: 'Could not load harbours' }),
            }),
          ),
        ),
      ),
    ),
  })),
);
