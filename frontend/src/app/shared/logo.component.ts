import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Logo « Relais » : deux points reliés, le conseiller et son client. */
@Component({
  selector: 'app-logo',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="logo">
      <svg viewBox="0 0 32 32" width="28" height="28" aria-hidden="true">
        <rect width="32" height="32" rx="9" class="tile" />
        <circle cx="11" cy="16" r="4.5" fill="#fff" />
        <circle cx="21" cy="16" r="4.5" fill="none" stroke="#fff" stroke-width="2.5" />
        <rect x="14" y="14.75" width="4" height="2.5" rx="1.2" fill="#fff" />
      </svg>
      @if (!compact()) {
        <span class="name">Relais</span>
      }
    </span>
  `,
  styles: `
    .logo { display: inline-flex; align-items: center; gap: 10px; }
    .tile { fill: var(--accent); }
    .name { font-weight: 700; font-size: 18px; letter-spacing: -0.02em; color: inherit; }
  `,
})
export class LogoComponent {
  readonly compact = input(false);
}
