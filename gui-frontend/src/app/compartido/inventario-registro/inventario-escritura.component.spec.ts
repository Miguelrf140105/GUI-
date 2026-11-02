import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NgModel } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { SharedModule } from '../shared.module';
import { InventarioRegistroComponent } from './inventario-registro.component';
import { AplicacionService } from '../../servicios/compartido/aplicacion.service';
import { InventarioService } from '../../servicios/inventario/inventario.service';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { InicioSesionService } from '../../servicios/usuarios/login/iniciar-sesion.service';

describe('Escritura en el formulario real de bienes', () => {
  beforeEach(async () => {
    const lookups: Record<string, any[]> = {
      families: [{ id: 44, name: 'Monitor' }, { id: 45, name: 'Mouse' }],
      products: [{ id: 636, familyId: 44, name: 'Monitor existente', brand: 'Marca', model: 'Modelo' }],
    };
    await TestBed.configureTestingModule({
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        { provide: AplicacionService, useValue: { options: (key: string) => lookups[key] || [] } },
        { provide: InventarioService, useValue: {} },
        { provide: CatalogosService, useValue: { pagina: () => of({ rows: [], hasMore: false }) } },
        { provide: InicioSesionService, useValue: { can: () => false } },
      ],
    }).compileComponents();
  });

  for (const transition of ['Mouse nuevo', 'Monitor reutilizado a Mouse', 'Monitor reutilizado a Nuevo']) {
    it(`permite escribir nombre, marca y modelo: ${transition}`, async () => {
      const fixture = TestBed.createComponent(InventarioRegistroComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();
      if (transition !== 'Mouse nuevo') {
        component.familyChanged(0, 44);
        component.modelChanged(0, 636);
        fixture.detectChanges();
        await fixture.whenStable();
      }
      if (transition === 'Monitor reutilizado a Nuevo') component.modelChanged(0, '');
      else component.familyChanged(0, 45);
      fixture.detectChanges();
      await fixture.whenStable();
      const key = component.lines[0].key;
      for (const field of ['name', 'brand', 'model'] as const) {
        const findInput = () => fixture.debugElement.queryAll(By.directive(NgModel))
          .find((element) => element.injector.get(NgModel).name === `${field}-${key}`)!.nativeElement as HTMLInputElement;
        const input = findInput();
        expect(input).not.toBeNull();
        expect(input.readOnly).toBeFalse();
        expect(input.disabled).toBeFalse();
        input.value = `Prueba ${field}`;
        input.dispatchEvent(new Event('input'));
        fixture.detectChanges();
        await fixture.whenStable();
        expect(component.lines[0][field]).toBe(`Prueba ${field}`);
        expect(findInput()).toBe(input);
      }
      fixture.destroy();
    });
  }
});
