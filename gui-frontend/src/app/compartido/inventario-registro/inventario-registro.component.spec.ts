import { fakeAsync, tick, TestBed } from '@angular/core/testing';
import { SharedModule } from '../shared.module';
import { AplicacionService } from '../../servicios/compartido/aplicacion.service';
import { InicioSesionService } from '../../servicios/usuarios/login/iniciar-sesion.service';
import { InventarioService } from '../../servicios/inventario/inventario.service';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { Subject, of } from 'rxjs';
import { InventarioRegistroComponent } from './inventario-registro.component';

describe('Botón de registro con formulario real y API simulada', () => {
  async function render(valid: boolean, failure = false) {
    const inventory = jasmine.createSpyObj('inventario', ['registrar']);
    const response = new Subject<any>();
    inventory.registrar.and.returnValue(response);
    const rows: any = {
      families: [{ id: 1, name: 'Microscopio' }],
      institutions: [{ id: 2, name: 'Proveedor' }],
      locations: [{ id: 3, name: 'Laboratorio', parentId: null }],
      movementTypes: [{ id: 4, name: 'Compra', mode: 1 }],
    };
    await TestBed.configureTestingModule({
      imports: [SharedModule],
      providers: [
        { provide: AplicacionService, useValue: { options: (key: string) => rows[key] || [] } },
        { provide: InicioSesionService, useValue: { can: () => true } },
        { provide: InventarioService, useValue: inventory },
        { provide: CatalogosService, useValue: {} },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(InventarioRegistroComponent);
    const c = fixture.componentInstance;
    c.lines[0] = { ...c.lines[0], familyId: 1, name: 'Microscopio electrónico', quantity: 24 };
    if (valid) Object.assign(c.header, { typeId: 4, institutionId: 2, locationId: 3 });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const saved = jasmine.createSpy('saved');
    c.saved.subscribe(saved);
    fixture.nativeElement.querySelector('button[type=submit]').click();
    fixture.detectChanges();
    if (valid) {
      if (failure) response.error(new Error('No se pudo guardar el lote.'));
      else response.next([{ id: 99 }]);
      await fixture.whenStable();
      fixture.detectChanges();
    }
    return { fixture, inventory, saved };
  }
  it('muestra los campos pendientes fuera del área desplazable sin enviar el lote', async () => {
    const { fixture, inventory } = await render(false);
    expect(inventory.registrar).not.toHaveBeenCalled();
    const alert = fixture.nativeElement.querySelector('[role=alert]');
    expect(alert.textContent).toContain('obligatorios');
    expect(alert.closest('.registration-body')).toBeNull();
  });
  it('envía las 24 unidades y comunica el guardado al inventario', async () => {
    const { inventory, saved } = await render(true);
    expect(inventory.registrar).toHaveBeenCalledWith(
      jasmine.objectContaining({
        lines: [jasmine.objectContaining({ quantity: 24, name: 'Microscopio electrónico' })],
      })
    );
    expect(saved).toHaveBeenCalledWith([{ id: 99 }]);
  });
  it('conserva el lote y permite reintentar cuando falla la API', async () => {
    const { fixture, saved } = await render(true, true);
    expect(saved).not.toHaveBeenCalled();
    expect(fixture.componentInstance.lines[0].quantity).toBe(24);
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain(
      'No se pudo guardar'
    );
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBeFalse();
  });
});
describe('Recepción Angular por RUC', () => {
  function setup() {
    const responses: Subject<any>[] = [];
    const inventory: any = {
      proveedor: jasmine.createSpy('proveedor').and.callFake(() => {
        const result = new Subject<any>();
        responses.push(result);
        return result;
      }),
    };
    const app: any = { options: () => [] };
    const auth: any = { can: () => true };
    const component = new InventarioRegistroComponent(app, auth, inventory, {} as any);
    component.ngOnInit();
    return { component, responses, inventory };
  }
  it('cancela búsquedas antiguas y utiliza los datos del RUC actual', fakeAsync(() => {
    const { component, responses } = setup();
    component.onDocument('11111111111');
    tick(400);
    component.onDocument('22222222222');
    responses[0].next({ found: true, institution: { id: 1, name: 'Anterior', stateId: 1 } });
    expect(component.matchedProvider).toBeNull();
    tick(400);
    responses[1].next({
      found: true,
      institution: { id: 2, name: 'Actual', document: '22222222222', stateId: 1 },
    });
    expect(component.provider.name).toBe('Actual');
    expect(component.effectiveInstitution).toBe(2);
    component.ngOnDestroy();
  }));
  it('impide continuar mientras busca o si el proveedor está inactivo', fakeAsync(() => {
    const { component, responses } = setup();
    component.onDocument('11111111111');
    expect(() => component.providerReady()).toThrowError(/Espera/);
    tick(400);
    responses[0].next({ found: true, institution: { id: 1, name: 'Proveedor', stateId: 0 } });
    expect(() => component.providerReady()).toThrowError(/inactivo/);
    component.ngOnDestroy();
  }));
  it('limpia datos recuperados al cambiar el RUC y conserva una línea al quitar', fakeAsync(() => {
    const { component, responses } = setup();
    component.onDocument('11111111111');
    tick(400);
    responses[0].next({
      found: true,
      institution: { id: 1, name: 'Proveedor', phone: '123', stateId: 1 },
    });
    component.onDocument('22222222222');
    expect(component.provider.name).toBe('');
    expect(component.provider.phone).toBe('');
    component.remove(component.lines[0]);
    expect(component.lines.length).toBe(1);
    component.ngOnDestroy();
    tick(400);
  }));
  it('vuelve a consultar el mismo RUC tras corregirlo rápidamente', fakeAsync(() => {
    const { component, responses, inventory } = setup();
    component.onDocument('11111111111');
    tick(400);
    responses[0].next({ found: true, institution: { id: 1, name: 'Proveedor', stateId: 1 } });
    component.onDocument('1111111111');
    tick(100);
    component.onDocument('11111111111');
    tick(400);
    expect(inventory.proveedor).toHaveBeenCalledTimes(2);
    responses[1].next({ found: true, institution: { id: 1, name: 'Proveedor', stateId: 1 } });
    expect(component.providerSearch).toBe('found');
    expect(component.effectiveInstitution).toBe(1);
    component.ngOnDestroy();
  }));
});

describe('Características y comprobantes de recepción', () => {
  it('registra características numéricas, incluido cero, como valores del contrato SQL', async () => {
    const catalogs: any = {
      options: (key: string) =>
        ((
          {
            familySpecifications: [{ familyId: 1, specificationId: 2, required: true }],
            specifications: [{ id: 2, dataType: 2, name: 'Capacidad' }],
          } as any
        )[key] || []),
    };
    const inventory = jasmine.createSpyObj('inventario', ['registrar']);
    inventory.registrar.and.returnValue(of([]));
    const component = new InventarioRegistroComponent(catalogs, {} as any, inventory, {} as any);
    component.lines[0] = {
      ...component.lines[0],
      familyId: 1,
      name: 'Equipo',
      specifications: { 2: 0 },
    };
    expect(component.finish(component.lines[0])).toBeTrue();
    await component.submit({ invalid: false } as any);
    expect(inventory.registrar).toHaveBeenCalledWith(
      jasmine.objectContaining({
        lines: [jasmine.objectContaining({ specifications: [{ id: 2, value: '0' }] })],
      })
    );
  });

  it('reintenta la misma página si falla la consulta de comprobantes y bloquea clics duplicados', async () => {
    const response = new Subject<any>();
    const api = jasmine.createSpyObj('catalogos', ['pagina']);
    api.pagina.and.returnValue(response);
    const component = new InventarioRegistroComponent({} as any, {} as any, {} as any, api);
    const first = component.loadDocuments();
    await component.loadDocuments();
    expect(api.pagina).toHaveBeenCalledTimes(1);
    response.error(new Error('Sin conexión'));
    await first;
    expect(component.documentPage).toBe(0);
    api.pagina.and.returnValue(of({ rows: [{ id: 1 }], hasMore: false }));
    await component.loadDocuments();
    expect(api.pagina.calls.mostRecent().args).toEqual([70, 0]);
    expect(component.existing.length).toBe(1);
  });
});

describe('Reutilización de modelo sin alterar la recepción pendiente', () => {
  it('transfiere el modelo y conserva cantidad, series, destino y comprobantes', () => {
    const c = new InventarioRegistroComponent(
      {
        options: (key: string) =>
          key === 'products'
            ? [
                {
                  id: 700,
                  familyId: 3,
                  name: 'Modelo de microscopio',
                  brand: 'LaborTech',
                  model: 'M1',
                },
              ]
            : key === 'families'
            ? [{ id: 3, name: 'Microscopio' }]
            : [],
      } as any,
      {} as any,
      {} as any,
      {} as any
    );
    c.lines[0] = { ...c.lines[0], familyId: 3, quantity: 24, serialList: 'SERIE' };
    c.header.locationId = 801;
    c.documentIds = [4];
    c.openModelPicker(c.lines[0]);
    c.chooseModel(c.modelChoices[0]);
    expect(c.lines[0].productId).toBe(700);
    expect(c.lines[0].name).toBe('Modelo de microscopio');
    expect(c.lines[0].quantity).toBe(24);
    expect(c.lines[0].serialList).toBe('SERIE');
    expect(c.header.locationId).toBe(801);
    expect(c.documentIds).toEqual([4]);
    expect(c.modelPickerLineKey).toBeNull();
  });
});
