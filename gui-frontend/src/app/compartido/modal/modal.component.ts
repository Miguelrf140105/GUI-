import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnDestroy,
  AfterViewInit,
  ElementRef,
  ViewChild,
  HostListener,
} from '@angular/core';

@Component({
  selector: 'gui-modal',
  templateUrl: './modal.component.html',
  styles: [':host { display: contents; }'],
})
export class ModalComponent implements AfterViewInit, OnDestroy {
  @Input() title = '';
  @Input() busy = false;
  @Output() close = new EventEmitter<void>();
  @ViewChild('dialog') dialog!: ElementRef<HTMLElement>;
  private previousOverflow = '';
  private previous: HTMLElement | null = null;
  onBackdropMouseDown(event: MouseEvent): void {
    // Un manejador Angular que devuelve false cancela la acción predeterminada:
    // los clics internos deben conservar el foco nativo de inputs y selects.
    if (event.target === event.currentTarget && !this.busy) this.close.emit();
  }
  ngAfterViewInit(): void {
    this.previous = document.activeElement as HTMLElement;
    this.previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    setTimeout(() => this.dialog.nativeElement.focus());
  }
  ngOnDestroy(): void {
    document.body.style.overflow = this.previousOverflow;
    this.previous?.focus();
  }
  @HostListener('document:keydown', ['$event']) key(event: KeyboardEvent): void {
    if (event.key === 'Escape' && !this.busy) {
      // PrimeNG cierra su panel antes de propagar Escape al documento.
      // El filtro puede estar fuera del diálogo por appendTo="body".
      const target = event.target;
      if (event.defaultPrevented ||
          (target instanceof Element && target.closest('p-dropdown, .p-dropdown-panel'))) return;
      event.preventDefault();
      this.close.emit();
    }
    if (event.key === 'Tab') {
      const nodes = Array.from(
        this.dialog.nativeElement.querySelectorAll<HTMLElement>(
          'button,input,select,textarea,a[href],[tabindex="0"]'
        )
      ).filter((node) => node.offsetParent !== null && !node.hasAttribute('disabled'));
      const first = nodes[0],
        last = nodes[nodes.length - 1];
      if (!first) {
        event.preventDefault();
        return;
      }
      if (
        event.shiftKey &&
        (document.activeElement === first || document.activeElement === this.dialog.nativeElement)
      ) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }
  }
}
