import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'gui-accordion',
  templateUrl: './accordion.component.html',
  styles: [':host { display: contents; }'],
})
export class AccordionComponent {
  @Input() title = '';
  @Input() summary = '';
  @Input() open = false;
  @Input() product = false;
  @Output() toggle = new EventEmitter<void>();
}
