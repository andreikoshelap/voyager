import { Component } from '@angular/core';
import { ChartMapComponent } from './chart-map.component';
import { HarbourPanelComponent } from './harbour-panel.component';

/** Container: full-bleed map with the info panel floating on top. */
@Component({
  selector: 'vl-chart-page',
  standalone: true,
  imports: [ChartMapComponent, HarbourPanelComponent],
  templateUrl: './chart-page.component.html',
  styleUrl: './chart-page.component.scss',
})
export class ChartPageComponent {}
