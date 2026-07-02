import { Component, computed, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { HarbourStore } from '../../state/harbour.store';
import { TripStore } from '../../state/trip.store';

/**
 * Reads HarbourStore.selected() directly — no @Input wiring to the map
 * component, both sit on the same store.
 */
@Component({
  selector: 'vl-harbour-panel',
  standalone: true,
  templateUrl: './harbour-panel.component.html',
  styleUrl: './harbour-panel.component.scss',
})
export class HarbourPanelComponent {
  protected readonly harbourStore = inject(HarbourStore);
  protected readonly tripStore = inject(TripStore);

  protected readonly atSeaCount = computed(
    () => this.tripStore.activeTrips().filter((t) => t.status !== 'ALERTED').length,
  );

  protected requestBerthUrl(): string {
    const h = this.harbourStore.selected();
    return `https://t.me/${environment.telegramBotUsername}?start=berth_${h?.id ?? ''}`;
  }

  protected sailHereUrl(): string {
    const h = this.harbourStore.selected();
    return `https://t.me/${environment.telegramBotUsername}?start=sail_${h?.id ?? ''}`;
  }
}
