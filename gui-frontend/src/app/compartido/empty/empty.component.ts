import { Component, Input } from '@angular/core';

@Component({
  selector: 'gui-empty',
  templateUrl: './empty.component.html',
  styles: [':host { display: contents; }'],
})
export class EmptyComponent {
  @Input() text = 'Aún no hay registros';
  @Input() detail = 'Agrega el primer registro para comenzar.';
}
