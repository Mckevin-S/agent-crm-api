import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { PRIORITY_LABELS, Priority } from '../../core/models';

@Component({
  selector: 'app-priority-chip',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span [class]="'chip ' + priority().toLowerCase()">{{ labels[priority()] }}</span>`,
  styles: `
    .chip { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 6px; font-size: 11px; font-weight: 700;
      text-transform: uppercase; letter-spacing: 0.04em; white-space: nowrap; }
    .high { background: var(--prio-high-bg); color: var(--prio-high); }
    .medium { background: var(--prio-medium-bg); color: var(--prio-medium); }
    .low { background: var(--prio-low-bg); color: var(--prio-low); }
  `,
})
export class PriorityChipComponent {
  readonly priority = input.required<Priority>();
  readonly labels = PRIORITY_LABELS;
}
