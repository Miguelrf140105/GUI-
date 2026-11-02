import { Component, Input } from '@angular/core';

@Component({
  selector: 'gui-badge',
  templateUrl: './badge.component.html',
  styles: [':host { display: contents; }'],
})
export class BadgeComponent {
  @Input() value: any;
  get color(): string {
    return /tránsito|Solicitado|mantenimiento|Autorizado/i.test(String(this.value))
      ? 'amber'
      : /Inactivo|baja|Donado/i.test(String(this.value))
      ? 'muted'
      : 'green';
  }
}
