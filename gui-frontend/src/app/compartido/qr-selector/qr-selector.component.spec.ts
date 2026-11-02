import { of, Subject, throwError } from 'rxjs';
import { QrSelectorComponent } from './qr-selector.component';
describe('Selector de impresión QR', () => {
  const asset = (id: number, name = 'Microscopio electrónico') => ({
    id,
    code: 'MIC-' + id,
    name,
    brand: 'LaborTech',
    model: 'M1',
    location: 'Laboratorio 801',
    locationId: 8,
    family: 'Microscopio',
    token: 'token' + id,
  });
  it('busca en todas las páginas autorizadas y agrupa productos por ubicación', async () => {
    const api = jasmine.createSpyObj('catalogos', ['pagina']);
    api.pagina.and.returnValues(
      of({ rows: [asset(1)], hasMore: true }),
      of({ rows: [asset(2)], hasMore: false })
    );
    const c = new QrSelectorComponent(api);
    await c.load();
    expect(api.pagina.calls.allArgs()).toEqual([
      [30, 0],
      [30, 1],
    ]);
    c.query = 'electronico LaborTech';
    expect(c.matches.length).toBe(2);
    expect(c.groups.length).toBe(1);
    c.toggleGroup(c.groups[0]);
    expect(c.selection.size).toBe(2);
    c.query = 'MIC-2';
    expect(c.matches.map((r) => r.id)).toEqual([2]);
    c.toggleGroup(c.groups[0]);
    expect(Array.from(c.selection.keys())).toEqual([1]);
    c.showPreview();
    expect(c.preview.map((r) => r.id)).toEqual([1]);
    c.preview = [];
    expect(c.selection.size).toBe(1);
  });
  it('mantiene la selección al cambiar filtros y no añade activos fuera de los resultados', () => {
    const c = new QrSelectorComponent({} as any);
    c.rows = [asset(1), { ...asset(2), location: 'Aula 802', family: 'Monitor' }];
    c.location = 'Laboratorio 801';
    c.addMatches();
    expect(Array.from(c.selection.keys())).toEqual([1]);
    c.resetFilters();
    c.family = 'Monitor';
    expect(c.matches.map((r) => r.id)).toEqual([2]);
    expect(c.selection.size).toBe(1);
  });
  it('no presenta una lista parcial como búsqueda completa cuando falla otra página', async () => {
    const api = jasmine.createSpyObj('catalogos', ['pagina']);
    api.pagina.and.returnValues(
      of({ rows: [asset(1)], hasMore: true }),
      throwError(() => new Error('Sin conexión'))
    );
    const c = new QrSelectorComponent(api);
    await c.load();
    expect(c.rows).toEqual([]);
    expect(c.error).toBe('Sin conexión');
    expect(c.loading).toBeFalse();
    c.selection.set(1, asset(1));
    c.showPreview();
    expect(c.preview).toEqual([]);
  });
  it('descarta la respuesta si se cierra el selector durante la carga', async () => {
    const response = new Subject<any>();
    const c = new QrSelectorComponent({ pagina: () => response } as any);
    const pending = c.load();
    c.ngOnDestroy();
    response.next({ rows: [asset(1)], hasMore: false });
    await pending;
    expect(c.rows).toEqual([]);
  });
});

describe('Buscador adaptado a cada acción', () => {
  const row = (id: number) => ({
    id,
    code: 'MIC-' + id,
    name: 'Microscopio',
    brand: 'LaborTech',
    model: 'M1',
    location: 'Laboratorio',
    family: 'Microscopio',
  });
  it('en movimientos y mantenimiento obliga a elegir una sola unidad del grupo', () => {
    for (const purpose of ['maintenance', 'edit', 'view', 'qr']) {
      const c = new QrSelectorComponent({} as any);
      c.mode = 'asset';
      c.purpose = purpose;
      c.rows = [row(1), row(2)];
      c.toggleGroup(c.groups[0]);
      expect(c.selection.size).toBe(0);
      expect(c.expanded.size).toBe(1);
      c.addMatches();
      expect(c.selection.size).toBe(0);
      c.toggle(row(1));
      c.toggle(row(2));
      expect(Array.from(c.selection.keys())).toEqual([2]);
      const picked = jasmine.createSpy('picked');
      c.picked.subscribe(picked);
      c.showPreview();
      expect(picked).toHaveBeenCalledWith(jasmine.objectContaining({ id: 2 }));
      expect(c.preview).toEqual([]);
    }
  });
  it('en modelos usa referencias del catálogo sin consultar ni seleccionar activos físicos', () => {
    const api = jasmine.createSpyObj('catalogos', ['pagina']);
    const c = new QrSelectorComponent(api);
    c.mode = 'model';
    c.providedRows = [
      { id: 500, name: 'Modelo reutilizable', brand: 'Marca', family: 'Monitor' },
      { id: 501, name: 'Otro modelo', family: 'Monitor' },
    ];
    c.ngOnInit();
    expect(api.pagina).not.toHaveBeenCalled();
    expect(c.groups.length).toBe(2);
    c.toggleGroup(c.groups[0]);
    c.toggleGroup(c.groups[1]);
    expect(Array.from(c.selection.keys())).toEqual([501]);
    const picked = jasmine.createSpy('picked');
    c.picked.subscribe(picked);
    c.showPreview();
    expect(picked).toHaveBeenCalledWith(jasmine.objectContaining({ id: 501 }));
    expect(c.help).toContain('nuevos códigos');
  });
  it('imprimir conserva la selección múltiple y la vista previa de etiquetas', () => {
    const c = new QrSelectorComponent({} as any);
    c.rows = [row(1), row(2)];
    c.toggleGroup(c.groups[0]);
    c.showPreview();
    expect(c.selection.size).toBe(2);
    expect(c.preview.length).toBe(2);
  });
});

describe('Acumulación de QR por activo', () => {
  const rows = [
    { id: 1, code: 'MIC-000001', token: 'ABC-123', name: 'Microscopio' },
    { id: 2, code: 'MIC-000002', token: 'DEF-456', name: 'Microscopio' },
  ];
  it('acumula por URL, token y lector USB sin duplicarse', () => {
    const c = new QrSelectorComponent({} as any);
    c.mode = 'asset';
    c.purpose = 'movement';
    c.rows = rows;
    c.scanValue('http://localhost:4200/a/abc-123');
    c.scanValue('MIC-000001');
    c.scanValue('def-456');
    expect(c.selection.size).toBe(2);
    c.scanValue('DEF-456');
    expect(c.selection.size).toBe(2);
    expect(c.scanMessage).toContain('ya está');
    const picked = jasmine.createSpy('picked');
    c.pickedMany.subscribe(picked);
    c.showPreview();
    expect(picked).toHaveBeenCalledWith(rows);
  });
  it('rechaza QR desconocidos y conserva la selección al quitar una unidad', () => {
    const c = new QrSelectorComponent({} as any);
    c.mode = 'asset';
    c.purpose = 'movement';
    c.rows = rows;
    c.scanValue('ABC-123');
    c.scanValue('DEF-456');
    c.scanValue('https://externo.invalid/ningun-activo');
    expect(c.selection.size).toBe(2);
    expect(c.scanError).toContain('alcance');
    c.toggle(rows[0]);
    expect(Array.from(c.selection.keys())).toEqual([2]);
  });
  it('consulta y edición mantienen una sola unidad aunque se lean dos QR', () => {
    const c = new QrSelectorComponent({} as any);
    c.mode = 'asset';
    c.purpose = 'edit';
    c.rows = rows;
    c.scanValue('ABC-123');
    c.scanValue('DEF-456');
    expect(Array.from(c.selection.keys())).toEqual([2]);
  });
});
