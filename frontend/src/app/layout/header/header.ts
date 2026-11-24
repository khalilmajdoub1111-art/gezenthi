import { ChangeDetectionStrategy, Component, HostListener, signal } from '@angular/core';
import { AGENCY_CONTACT, MAIN_NAV } from '../../core/data/navigation';
import { Icon } from '../../shared/icon/icon';

@Component({
  selector: 'app-header',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  // 'contents' so the sticky <header> sticks relative to the whole page, not this host box.
  host: { class: 'contents' },
  templateUrl: './header.html',
})
export class Header {
  protected readonly nav = MAIN_NAV;
  protected readonly contact = AGENCY_CONTACT;

  protected readonly scrolled = signal(false);
  protected readonly mobileOpen = signal(false);
  protected readonly mobileExpanded = signal<string | null>(null);

  @HostListener('window:scroll')
  onScroll(): void {
    this.scrolled.set(window.scrollY > 24);
  }

  @HostListener('document:keydown.escape')
  closeMobile(): void {
    this.mobileOpen.set(false);
    document.body.style.overflow = '';
  }

  toggleMobile(): void {
    const open = !this.mobileOpen();
    this.mobileOpen.set(open);
    document.body.style.overflow = open ? 'hidden' : '';
  }

  toggleSection(label: string): void {
    this.mobileExpanded.update((current) => (current === label ? null : label));
  }
}
