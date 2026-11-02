import { Subject } from 'rxjs';
import { ListadoComponent } from './listado.component';

describe('Búsqueda de activos', () => {
  it('conserva el último resultado cuando las respuestas llegan fuera de orden', async () => {
    const old = new Subject<any>(),
      latest = new Subject<any>();
    const inventory = jasmine.createSpyObj('inventario', ['buscar']);
    inventory.buscar.and.returnValues(old, latest);
    const list = new ListadoComponent({} as any, {} as any, {} as any, inventory);
    const first = list.findAsset();
    const second = list.findAsset();
    latest.next({ id: 2 });
    latest.complete();
    await second;
    old.next({ id: 1 });
    old.complete();
    await first;
    expect(list.selected?.id).toBe(2);
  });

  it('retira la ficha anterior cuando falla una nueva búsqueda', async () => {
    const result = new Subject<any>();
    const list = new ListadoComponent(
      {} as any,
      {} as any,
      {} as any,
      { buscar: () => result } as any
    );
    list.selected = { id: 1 };
    const loading = list.findAsset();
    result.error(new Error('No encontrado'));
    await loading;
    expect(list.selected).toBeNull();
    expect(list.error).toBe('No encontrado');
  });
});

describe('Filtros por columna de inventario', () => {
  function setup() {
    const list = new ListadoComponent({} as any, {} as any, {} as any, {} as any);
    list.result.rows = [
      {
        id: 1,
        code: 'MIC-000001',
        name: 'Microscopio electrónico',
        family: 'Microscopio',
        serial: null,
        location: 'Laboratorio 801',
        responsible: 'Ana',
        status: 'Operativo',
      },
      {
        id: 2,
        code: 'MIC-000002',
        name: 'Microscopio electrónico',
        family: 'Microscopio',
        serial: 'ABC',
        location: 'Laboratorio 802',
        responsible: 'Luis',
        status: 'En revisión',
      },
      {
        id: 3,
        code: 'MON-000001',
        name: 'Pantalla',
        family: 'Monitor',
        serial: 'XYZ',
        location: 'Laboratorio 801',
        responsible: 'Ana',
        status: 'Operativo',
      },
    ];
    return list;
  }
  it('incluye el tipo de producto sin mostrar el ID interno', () => {
    const list = setup();
    expect(list.columns.some((c) => c.key === 'family')).toBeTrue();
    expect(list.columns.some((c) => c.key === 'id')).toBeFalse();
  });
  it('combina tipo, ubicación y responsable ignorando mayúsculas y tildes', () => {
    const list = setup();
    list.columnQueries = {
      family: ' MICROSCOPIO ',
      location: '801',
      responsible: 'ana',
      name: 'electronico',
    };
    expect(list.filtered.map((row) => row.id)).toEqual([1]);
  });
  it('combina el buscador general con código y permite filtrar series vacías', () => {
    const list = setup();
    list.query = 'Microscopio';
    list.columnQueries = { code: '000002' };
    expect(list.filtered.map((row) => row.id)).toEqual([2]);
    list.columnQueries = { serial: '—' };
    expect(list.filtered.map((row) => row.id)).toEqual([1]);
  });
  it('limpia la búsqueda y los filtros sin cambiar los datos', () => {
    const list = setup();
    list.columnQueries = { status: 'no existe' };
    list.query = 'algo';
    list.filter = '1';
    expect(list.filtered.length).toBe(0);
    list.clearInventoryFilters();
    expect(list.hasColumnFilters).toBeFalse();
    expect(list.filtered.length).toBe(3);
    list.moduleId = 40;
    list.columnQueries = { code: 'no existe' };
    expect(list.filtered.length).toBe(3);
  });
});

describe('Acciones desde el buscador de productos', () => {
  const row = { id: 24, code: 'MIC-000024', name: 'Microscopio' };
  it('solo prepara movimiento o mantenimiento con el activo elegido sin guardar', () => {
    const api = jasmine.createSpyObj('catalogos', ['guardar']);
    const c = new ListadoComponent({} as any, { can: () => true } as any, api, {} as any);
    for (const action of ['movement', 'maintenance']) {
      c.pickerAction = action;
      c.actionPickerOpen = true;
      c.chooseForAction(row);
      expect(c.modal?.kind).toBe(action);
      expect(c.modal?.asset).toBe(row);
      expect(c.modal?.row).toBeUndefined();
      expect(c.actionPickerOpen).toBeFalse();
    }
    expect(api.guardar).not.toHaveBeenCalled();
  });
  it('consulta y edita un solo activo, y respeta los permisos disponibles', () => {
    const c = new ListadoComponent({} as any, { can: () => true } as any, {} as any, {} as any);
    c.pickerAction = 'view';
    c.chooseForAction(row);
    expect(c.selected).toBe(row);
    c.pickerAction = 'edit';
    c.chooseForAction(row);
    expect(c.modal?.kind).toBe('asset-name');
    expect(c.modal?.row).toBe(row);
    const denied = new ListadoComponent(
      {} as any,
      { can: () => false } as any,
      {} as any,
      {} as any
    );
    denied.pickerAction = 'edit';
    denied.chooseForAction(row);
    expect(denied.modal).toBeNull();
  });
});

describe('Eliminar ubicaciones con permisos', () => {
  function setup(allowed = true) {
    const api = jasmine.createSpyObj('api', ['guardar']);
    const app = jasmine.createSpyObj('app', ['refresh']);
    app.refresh.and.returnValue(Promise.resolve());
    const list = new ListadoComponent(
      app,
      { can: (id: number) => id === 503 && allowed } as any,
      api,
      {} as any
    );
    list.moduleId = 50;
    list.locationTree = [
      { id: 1, name: 'Sede', parentId: null },
      { id: 2, name: 'Piso', parentId: 1, quantity: 0 },
    ];
    return { list, api, app };
  }
  it('no permite iniciar ni confirmar sin permiso 503', async () => {
    const { list, api } = setup(false);
    list.requestLocationDelete({ id: 2 });
    expect(list.locationDelete).toBeNull();
    list.locationDelete = { id: 2 };
    await list.deleteLocation();
    expect(api.guardar).not.toHaveBeenCalled();
  });
  it('bloquea espacios hijos y activos antes de enviar', async () => {
    const { list, api } = setup();
    list.requestLocationDelete(list.locationTree[0]);
    await list.deleteLocation();
    expect(list.locationDeleteBlock).toContain('otros espacios');
    list.locationTree[1].quantity = 1;
    list.requestLocationDelete(list.locationTree[1]);
    await list.deleteLocation();
    expect(list.locationDeleteBlock).toContain('activos');
    expect(api.guardar).not.toHaveBeenCalled();
  });
  it('confirma una sola vez, limpia el filtro y recarga el árbol', async () => {
    const { list, api, app } = setup();
    const result = new Subject<any>();
    api.guardar.and.returnValue(result);
    list.locationFilterId = 2;
    list.requestLocationDelete(list.locationTree[1]);
    const first = list.deleteLocation();
    await list.deleteLocation();
    expect(api.guardar).toHaveBeenCalledOnceWith('/locations/2/delete', {});
    result.next({ id: 2 });
    result.complete();
    await first;
    expect(list.locationDelete).toBeNull();
    expect(list.locationFilterId).toBeNull();
    expect(app.refresh).toHaveBeenCalled();
    expect(list.locationDeleteBusy).toBeFalse();
  });
  it('mantiene el diálogo y muestra el rechazo del servidor', async () => {
    const { list, api } = setup();
    const result = new Subject<any>();
    api.guardar.and.returnValue(result);
    list.requestLocationDelete(list.locationTree[1]);
    const saving = list.deleteLocation();
    result.error(new Error('Tiene movimientos pendientes'));
    await saving;
    expect(list.locationDelete?.id).toBe(2);
    expect(list.locationDeleteError).toContain('movimientos pendientes');
  });
});

describe('Edición exclusiva del Administrador', () => {
  it('oculta y rechaza editar en todos los listados aunque tenga permisos de gestión', () => {
    const list = new ListadoComponent(
      {} as any,
      { can: (id: number) => id !== 504 } as any,
      {} as any,
      {} as any
    );
    for (const moduleId of [50, 90, 100, 130]) {
      list.moduleId = moduleId;
      expect(list.canEdit({ id: 2 })).toBeFalse();
    }
    for (const kind of ['location', 'user', 'role', 'institution', 'asset-name']) {
      list.open(kind, { id: 2 });
      expect(list.modal).toBeNull();
    }
    list.pickerAction = 'edit';
    list.chooseForAction({ id: 24 });
    expect(list.modal).toBeNull();
  });
  it('conserva la creación autorizada y permite editar al Administrador', () => {
    const list = new ListadoComponent({} as any, { can: () => true } as any, {} as any, {} as any);
    for (const moduleId of [50, 90, 100, 130]) {
      list.moduleId = moduleId;
      expect(list.canEdit({ id: 2 })).toBeTrue();
    }
    list.open('location', { id: 2 });
    expect(list.modal?.row?.id).toBe(2);
    const regular = new ListadoComponent(
      {} as any,
      { can: (id: number) => id === 502 } as any,
      {} as any,
      {} as any
    );
    regular.moduleId = 50;
    regular.open('location');
    expect(regular.modal?.kind).toBe('location');
  });
});

describe('Borrado lógico de proveedores', () => {
  function setup(allowed = true) {
    const api = jasmine.createSpyObj('api', ['guardar']);
    const app = jasmine.createSpyObj('app', ['refresh']);
    app.refresh.and.returnValue(Promise.resolve());
    const list = new ListadoComponent(
      app,
      { can: (id: number) => id === 1303 && allowed } as any,
      api,
      {} as any
    );
    list.moduleId = 130;
    return { list, api, app };
  }
  it('no permite iniciar ni confirmar sin permiso administrativo de baja', async () => {
    const { list, api } = setup(false);
    list.requestProviderDelete({ id: 1, stateId: 1 });
    expect(list.providerDelete).toBeNull();
    list.providerDelete = { id: 1, stateId: 1 };
    await list.deleteProvider();
    expect(api.guardar).not.toHaveBeenCalled();
  });
  it('evita doble envío y refresca listado y catálogos tras confirmar', async () => {
    const { list, api, app } = setup();
    const result = new Subject<any>();
    api.guardar.and.returnValue(result);
    list.requestProviderDelete({ id: 3, name: 'QA', stateId: 1 });
    const first = list.deleteProvider();
    await list.deleteProvider();
    expect(api.guardar).toHaveBeenCalledOnceWith('/institutions/3/delete', {});
    result.next({ id: 3 });
    result.complete();
    await first;
    expect(list.providerDelete).toBeNull();
    expect(app.refresh).toHaveBeenCalled();
    expect(list.providerDeleteBusy).toBeFalse();
  });
  it('permite cancelar sin escribir y no ofrece borrar un eliminado', () => {
    const { list, api } = setup();
    expect(list.canDeleteProvider({ id: 1, stateId: 2 })).toBeFalse();
    list.requestProviderDelete({ id: 3, stateId: 1 });
    list.providerDelete = null;
    expect(api.guardar).not.toHaveBeenCalled();
  });
  it('conserva el diálogo con el motivo si el servidor rechaza la baja', async () => {
    const { list, api } = setup();
    const result = new Subject<any>();
    api.guardar.and.returnValue(result);
    list.requestProviderDelete({ id: 3, stateId: 1 });
    const pending = list.deleteProvider();
    result.error(new Error('No autorizado'));
    await pending;
    expect(list.providerDelete?.id).toBe(3);
    expect(list.providerDeleteError).toBe('No autorizado');
    expect(list.providerDeleteBusy).toBeFalse();
  });
});
