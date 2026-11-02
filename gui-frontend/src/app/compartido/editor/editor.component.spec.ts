import { Subject, of, throwError } from 'rxjs';
import { EditorComponent } from './editor.component';
import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { AplicacionService } from '../../servicios/compartido/aplicacion.service';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { SharedModule } from '../shared.module';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('Carga segura del editor', () => {
  it('distingue ambientes del mismo nombre por sede y piso sin cambiar sus IDs', () => {
    const locations = [
      { id: 1, name: 'UNID' },
      { id: 2, parentId: 1, name: 'Piso 1' },
      { id: 3, parentId: 1, name: 'Piso 2' },
      { id: 4, parentId: 2, name: 'Hall de ascensor' },
      { id: 5, parentId: 3, name: 'Hall de ascensor' },
    ];
    const app = {
      options: (key: string) =>
        key === 'locations' ? locations : key === 'movementTypes' ? [{ id: 10, mode: 2 }] : [],
    } as any;
    const editor = new EditorComponent(app, {} as any);
    editor.kind = 'movement';
    editor.form = { typeId: 10 };
    const destinations = editor.fields.find((field) => field.key === 'locationId')!.options!;
    expect(destinations.find((item) => item.id === 4)!.parentId).toBe(2);
    expect(destinations.find((item) => item.id === 5)!.parentId).toBe(3);
    expect(editor.fields.find((field) => field.key === 'locationId')!.type).toBe('location');
    editor.kind = 'location';
    editor.row = { id: 2 };
    const parents = editor.fields.find((field) => field.key === 'parentId')!.options!;
    expect(parents.map((item) => item.id)).toEqual([1, 3, 5]);
    expect(locations[3].name).toBe('Hall de ascensor');
  });
  it('impide borrar permisos guardando antes de terminar la carga', async () => {
    const permissions = new Subject<any[]>();
    const api = jasmine.createSpyObj('catalogos', ['permisos', 'guardar']);
    api.permisos.and.returnValue(permissions);
    api.guardar.and.returnValue(of({ id: 2 }));
    const editor = new EditorComponent({ options: () => [] } as any, api);
    editor.kind = 'role';
    editor.row = { id: 2, name: 'Consulta' };
    const loading = editor.ngOnInit();
    await editor.submit({ invalid: false } as any);
    expect(api.guardar).not.toHaveBeenCalled();
    permissions.next([{ id: 301 }]);
    permissions.complete();
    await loading;
    await editor.submit({ invalid: false } as any);
    expect(api.guardar).toHaveBeenCalledWith(
      '/roles',
      jasmine.objectContaining({ permissionIds: [301] })
    );
  });

  it('bloquea guardar si falla la lectura de las asignaciones del usuario', async () => {
    const api = jasmine.createSpyObj('catalogos', ['asignaciones', 'guardar']);
    api.asignaciones.and.returnValue(throwError(() => new Error('Sin conexión')));
    const editor = new EditorComponent({ options: () => [] } as any, api);
    editor.kind = 'user';
    editor.row = { id: 3 };
    await editor.ngOnInit();
    await editor.submit({ invalid: false } as any);
    expect(editor.error).toBe('Sin conexión');
    expect(api.guardar).not.toHaveBeenCalled();
  });

  it('elige una unidad sin perder motivo, destino ni fecha del formulario', async () => {
    const editor = new EditorComponent({} as any, {} as any);
    editor.form = { reason: 'Traslado de laboratorio', locationId: 801, date: '2026-10-26' };
    editor.assetPickerOpen = true;
    editor.chooseAsset({ id: 24, code: 'MIC-000024' });
    expect(editor.form.assetId).toBe(24);
    expect(editor.form.reason).toBe('Traslado de laboratorio');
    expect(editor.form.locationId).toBe(801);
    expect(editor.form.date).toBe('2026-10-26');
    expect(editor.assetPickerOpen).toBeFalse();
  });
});

describe('Formularios del editor con plantilla real', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule, SharedModule, NoopAnimationsModule],
      providers: [
        { provide: AplicacionService, useValue: { lookups: {}, options: () => [] } },
        { provide: CatalogosService, useValue: { pagina: () => of({ rows: [], hasMore: false }) } },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  });

  for (const kind of [
    'institution',
    'location',
    'movement',
    'maintenance',
    'document',
    'parameter',
    'family',
    'role',
    'user',
  ]) {
    it(`abre ${kind} sin reconstruir los controles en cada ciclo`, async () => {
      const fixture = TestBed.createComponent(EditorComponent);
      fixture.componentInstance.kind = kind;
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
      const control = fixture.nativeElement.querySelector('input,select,textarea');
      expect(control).not.toBeNull();
      fixture.detectChanges();
      await fixture.whenStable();
      expect(fixture.nativeElement.querySelector('input,select,textarea')).toBe(control);
      expect(fixture.componentInstance.ready).toBeTrue();
      fixture.destroy();
    });
  }

  it('marca el correo inválido antes de enviar el formulario', async () => {
    const fixture = TestBed.createComponent(EditorComponent);
    fixture.componentInstance.kind = 'institution';
    fixture.detectChanges();
    await fixture.whenStable();
    const email = fixture.nativeElement.querySelector('input[type=email]') as HTMLInputElement;
    email.value = 'correo-invalido';
    email.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(email.classList.contains('ng-invalid')).toBeTrue();
    fixture.destroy();
  });
});

describe('Envío de movimiento por lote', () => {
  it('envía una sola solicitud con activos únicos y conserva su clave al reintentar', async () => {
    const api = jasmine.createSpyObj('catalogos', ['guardar']);
    api.guardar.and.returnValue(throwError(() => new Error('Sin respuesta')));
    const editor = new EditorComponent(
      {
        options: (key: string) =>
          key === 'movementTypes' ? [{ id: 2, mode: 2, institutionRequired: false }] : [],
      } as any,
      api
    );
    editor.kind = 'movement';
    editor.ready = true;
    editor.form = { typeId: 2, locationId: 9, reason: 'Traslado' };
    editor.chooseAssets([
      { id: 2, code: 'MIC-2' },
      { id: 1, code: 'MIC-1' },
      { id: 2, code: 'MIC-2' },
    ]);
    await editor.submit({ invalid: false } as any);
    await editor.submit({ invalid: false } as any);
    const first = api.guardar.calls.argsFor(0),
      second = api.guardar.calls.argsFor(1);
    expect(first[0]).toBe('/movements/batch');
    expect(first[1].assetIds).toEqual([1, 2]);
    expect(first[1].assetId).toBeUndefined();
    expect(first[1].idempotencyKey).toBe(second[1].idempotencyKey);
    expect(editor.chosenAssets.length).toBe(2);
    expect(editor.form.reason).toBe('Traslado');
  });
});
