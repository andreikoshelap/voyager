import { Component, computed, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { HarbourStore } from '../../state/harbour.store';
import { TripStore } from '../../state/trip.store';

/**
 * UPDATED: trip list now shows the departure harbour name (looked up
 * client-side from HarbourStore by departureHarbourId) and marks
 * APPROXIMATE trips with a "≈" prefix on the destination label, matching
 * the dashed marker style on the map.
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

  protected etaLabel(iso: string): string {
    const d = new Date(iso);
    return d.toLocaleString('en-GB', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
  }

  protected departureName(harbourId: number | null): string {
    if (harbourId == null) return 'unknown harbour';
    return this.harbourStore.harbours().find((h) => h.id === harbourId)?.name ?? 'unknown harbour';
  }
}
