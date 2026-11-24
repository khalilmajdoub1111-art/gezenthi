import { Directive, ElementRef, OnDestroy, OnInit, inject, input } from '@angular/core';

/** Fades an element in when it scrolls into view. Usage: <div appReveal [revealDelay]="150">. */
@Directive({ selector: '[appReveal]' })
export class RevealDirective implements OnInit, OnDestroy {
  readonly revealDelay = input(0);
  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);
  private observer?: IntersectionObserver;

  ngOnInit(): void {
    const node = this.el.nativeElement;
    if (typeof IntersectionObserver === 'undefined') return;
    node.classList.add('reveal');
    node.style.transitionDelay = `${this.revealDelay()}ms`;
    this.observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          node.classList.add('is-visible');
          this.observer?.disconnect();
        }
      },
      { threshold: 0.12, rootMargin: '0px 0px -40px 0px' },
    );
    this.observer.observe(node);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
