import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { InicioSesionService } from '../../servicios/usuarios/login/iniciar-sesion.service';
import { createExcelWorkbook } from '../listado/excel-export';
import { of, Subject } from 'rxjs';
import { EgresoDetalleComponent } from './egreso-detalle.component';
import { ListadoComponent } from '../listado/listado.component';
describe('Egresos', () => {
  it('combina búsqueda, tipo y fechas al exportar las filas filtradas', () => {
    const c = new ListadoComponent({} as any, {} as any, {} as any, {} as any);
    c.moduleId = 150;
    c.result.rows = [
      { id: 1, code: 'EGR0001', typeId: 4, date: '2026-10-27', institution: 'UNID' },
      { id: 2, code: 'EGR0002', typeId: 7, date: '2026-10-28', institution: 'Baja' },
      { id: 3, code: 'EGR0003', typeId: 4, date: '2026-09-20', institution: 'UNID' },
    ];
    c.query = 'UNID';
    c.egressType = '4';
    c.dateFrom = '2026-10-01';
    c.dateTo = '2026-10-31';
    expect(c.filtered.map((x) => x.code)).toEqual(['EGR0001']);
  });
  it('consulta el detalle sin crear movimientos ni egresos', async () => {
    const api = jasmine.createSpyObj('api', ['egreso', 'pagina', 'guardar']);
    api.egreso.and.returnValue(of({ units: [{ code: 'MIC-000001' }], documents: [] }));
    const c = new EgresoDetalleComponent(api, { can: () => false } as any);
    c.row = { id: 1 };
    await c.ngOnInit();
    expect(c.detail?.units.length).toBe(1);
    expect(api.guardar).not.toHaveBeenCalled();
    expect(api.pagina).not.toHaveBeenCalled();
    c.documentId = 2;
    await c.associate();
    expect(api.guardar).not.toHaveBeenCalled();
  });
  it('bloquea doble asociación mientras la primera está en curso', async () => {
    const api = jasmine.createSpyObj('api', ['guardar', 'egreso']);
    const pending = new Subject<any>();
    api.guardar.and.returnValue(pending);
    api.egreso.and.returnValue(of({ units: [], documents: [{ id: 2 }] }));
    const c = new EgresoDetalleComponent(api, { can: () => true } as any);
    c.row = { id: 1 };
    c.documentId = 2;
    const first = c.associate();
    await c.associate();
    expect(api.guardar).toHaveBeenCalledTimes(1);
    pending.next({});
    pending.complete();
    await first;
    expect(c.busy).toBeFalse();
    expect(c.detail?.documents.length).toBe(1);
  });
});

describe('Egresos: detalle renderizado y Excel', () => {
  it('muestra códigos individuales sin controles de asociación a un lector', async () => {
    const api = {
      egreso: () =>
        of({
          units: [
            {
              code: 'MIC-000001',
              name: 'Microscopio',
              reason: 'Donación',
              date: '2026-10-27T15:00:00Z',
            },
            {
              code: 'MIC-000002',
              name: 'Microscopio',
              reason: 'Donación',
              date: '2026-10-27T15:00:00Z',
            },
          ],
          documents: [],
        }),
    };
    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule],
      declarations: [EgresoDetalleComponent],
      providers: [
        { provide: CatalogosService, useValue: api },
        { provide: InicioSesionService, useValue: { can: () => false } },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
    const fixture = TestBed.createComponent(EgresoDetalleComponent);
    fixture.componentInstance.row = {
      id: 1,
      code: 'EGR0001',
      type: 'Donación entregada',
      date: '2026-10-27',
    };
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('MIC-000001');
    expect(element.textContent).toContain('MIC-000002');
    expect(element.querySelector('select')).toBeNull();
  });
  it('exporta solo los egresos filtrados con código de texto, fecha y cantidades numéricas', async () => {
    const c = new ListadoComponent({} as any, {} as any, {} as any, {} as any);
    c.moduleId = 150;
    c.result.rows = [
      { code: 'EGR0001', date: '2026-10-27', quantity: 24, typeId: 4, documents: 1 },
      { code: 'EGR0002', date: '2026-10-28', quantity: 1, typeId: 7, documents: 0 },
    ];
    c.egressType = '4';
    const workbook = await createExcelWorkbook(c.filtered, c.columns, 'Egresos', 150);
    const sheet = workbook.worksheets[0];
    expect(sheet.rowCount).toBe(2);
    expect(sheet.getCell('A2').value).toBe('EGR0001');
    expect(sheet.getCell('B2').value instanceof Date).toBeTrue();
    expect(sheet.getCell('E2').value).toBe(24);
    expect(sheet.getCell('F2').value).toBe(1);
  });
});
