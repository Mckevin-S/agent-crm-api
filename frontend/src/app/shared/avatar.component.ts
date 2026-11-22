import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

const PALETTE = ['#0E7C7B', '#2F6FEB', '#C2410C', '#7C3AED', '#BE185D', '#0369A1', '#B7791F'];

export interface Person {
  id: number | string;
  fullName: string;
  initials: string;
}

/** Pastille aux initiales, couleur stable par personne. */
@Component({
  selector: 'app-avatar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (person(); as p) {
      <span class="avatar" [style.background]="color()" [style.width.px]="size()" [style.height.px]="size()"
            [style.font-size.px]="size() * 0.4" [attr.title]="p.fullName" [attr.aria-label]="p.fullName">{{ p.initials }}</span>
    } @else {
      <span class="avatar empty" [style.width.px]="size()" [style.height.px]="size()" aria-label="Personne">?</span>
    }
  `,
  styles: `
    .avatar { display: inline-flex; align-items: center; justify-content: center; border-radius: 50%;
      color: #fff; font-weight: 600; flex-shrink: 0; letter-spacing: 0.02em; user-select: none; }
    .empty { background: transparent; border: 1.5px dashed var(--border-strong); color: var(--muted); font-size: 11px; }
  `,
})
export class AvatarComponent {
  readonly person = input<Person | null | undefined>(null);
  readonly size = input(28);
  readonly color = computed(() => {
    const id = String(this.person()?.id ?? '');
    let hash = 0;
    for (const char of id) {
      hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
    }
    return PALETTE[hash % PALETTE.length];
  });
}
