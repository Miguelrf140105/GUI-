import { ModalComponent } from './modal.component';
import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

@Component({ template: '<gui-modal title="Prueba"><input aria-label="Nombre"><select><option>Mouse</option></select><textarea></textarea></gui-modal>' })
class ModalFocusHost {}

describe('Clics dentro de la ventana de formulario', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ModalComponent, ModalFocusHost],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  });
  it('no cancela el mousedown de los campos y permite su acción nativa de foco', () => {
    const fixture = TestBed.createComponent(ModalFocusHost);
    fixture.detectChanges();
    const modal = fixture.debugElement.query(By.directive(ModalComponent)).componentInstance as ModalComponent;
    spyOn(modal.close, 'emit');
    for (const field of fixture.nativeElement.querySelectorAll('input,select,textarea')) {
      const event = new MouseEvent('mousedown', { bubbles: true, cancelable: true });
      field.dispatchEvent(event);
      expect(event.defaultPrevented).toBeFalse();
    }
    expect(modal.close.emit).not.toHaveBeenCalled();
    fixture.destroy();
  });
  it('solo cierra al pulsar el fondo, y conserva el bloqueo durante el guardado', () => {
    const fixture = TestBed.createComponent(ModalFocusHost);
    fixture.detectChanges();
    const modal = fixture.debugElement.query(By.directive(ModalComponent)).componentInstance as ModalComponent;
    spyOn(modal.close, 'emit');
    const backdrop = fixture.nativeElement.querySelector('.modal-overlay');
    backdrop.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
    expect(modal.close.emit).toHaveBeenCalledTimes(1);
    modal.busy = true;
    backdrop.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
    expect(modal.close.emit).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });
});

describe('Escape en formularios con buscadores', () => {
  function escapeFrom(target: HTMLElement, component: ModalComponent): void {
    const listener = (event: KeyboardEvent) => component.key(event);
    document.addEventListener('keydown', listener);
    try {
      target.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }));
    } finally {
      document.removeEventListener('keydown', listener);
    }
  }
  it('conserva el formulario al cerrar el filtro desplegado fuera del diálogo', () => {
    const panel = document.createElement('div');
    panel.className = 'p-dropdown-panel';
    const filter = document.createElement('input');
    panel.appendChild(filter);
    document.body.appendChild(panel);
    const component = new ModalComponent();
    spyOn(component.close, 'emit');
    try {
      escapeFrom(filter, component);
      expect(component.close.emit).not.toHaveBeenCalled();
    } finally { panel.remove(); }
  });
  it('conserva el formulario si Escape procede del selector', () => {
    const dropdown = document.createElement('p-dropdown');
    const input = document.createElement('input');
    dropdown.appendChild(input);
    document.body.appendChild(dropdown);
    const component = new ModalComponent();
    spyOn(component.close, 'emit');
    try {
      escapeFrom(input, component);
      expect(component.close.emit).not.toHaveBeenCalled();
    } finally { dropdown.remove(); }
  });
  it('cierra con Escape fuera del selector y bloquea el cierre al guardar', () => {
    const component = new ModalComponent();
    spyOn(component.close, 'emit');
    escapeFrom(document.body, component);
    expect(component.close.emit).toHaveBeenCalledTimes(1);
    component.busy = true;
    escapeFrom(document.body, component);
    expect(component.close.emit).toHaveBeenCalledTimes(1);
  });
});
