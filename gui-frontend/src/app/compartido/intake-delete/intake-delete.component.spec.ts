import { Subject } from 'rxjs';
import { IntakeDeleteComponent } from './intake-delete.component';
describe('Advertencia y anulación de ingreso', () => {
  function setup(allowed = true) {
    const api = jasmine.createSpyObj('api', ['guardar']);
    const c = new IntakeDeleteComponent(api, { can: () => allowed } as any);
    c.row = { id: 2, code: 'ING0002' };
    return { c, api };
  }
  const detail = {
    lines: [{ quantity: 2 }],
    documents: [],
    units: [
      { id: 1, code: 'QA-1' },
      { id: 2, code: 'QA-2' },
    ],
  };
  it('no envía nada hasta cargar todas las unidades ni sin permiso', async () => {
    const { c, api } = setup();
    await c.submit();
    expect(api.guardar).not.toHaveBeenCalled();
    c.detail = { lines: [{ quantity: 2 }], documents: [], units: [{ id: 1 }] };
    await c.submit();
    expect(api.guardar).not.toHaveBeenCalled();
    const denied = setup(false);
    denied.c.detail = detail;
    await denied.c.submit();
    expect(denied.api.guardar).not.toHaveBeenCalled();
  });
  it('muestra el nombre y código del activo bloqueado y no anula una parte', async () => {
    const { c, api } = setup();
    c.detail = {
      ...detail,
      units: [
        { id: 1 },
        {
          id: 2,
          blockReason:
            'El producto Microscopio (MIC-000002) tuvo movimientos; no se puede eliminar.',
        },
      ],
    };
    expect(c.blockReason).toContain('MIC-000002');
    await c.submit();
    expect(api.guardar).not.toHaveBeenCalled();
  });
  it('una confirmación genera una sola petición para todo el ingreso', async () => {
    const { c, api } = setup();
    c.detail = detail;
    const result = new Subject<any>();
    api.guardar.and.returnValue(result);
    const saved = spyOn(c.saved, 'emit');
    const first = c.submit();
    await c.submit();
    expect(api.guardar).toHaveBeenCalledOnceWith('/intakes/2/delete', {});
    result.next({ id: 2 });
    result.complete();
    await first;
    expect(saved).toHaveBeenCalledTimes(1);
    expect(c.busy).toBeFalse();
  });
  it('presenta el rechazo si una unidad se movió después de abrir la advertencia', async () => {
    const { c, api } = setup();
    c.detail = detail;
    const result = new Subject<any>();
    api.guardar.and.returnValue(result);
    const saved = spyOn(c.saved, 'emit');
    const first = c.submit();
    result.error(new Error('El producto X tuvo movimientos; no se puede eliminar.'));
    await first;
    expect(c.error).toContain('tuvo movimientos');
    expect(saved).not.toHaveBeenCalled();
    expect(c.busy).toBeFalse();
  });
  it('cancelar la advertencia no modifica el ingreso', () => {
    const { c, api } = setup();
    c.detail = detail;
    const cancel = spyOn(c.cancel, 'emit');
    c.cancel.emit();
    expect(cancel).toHaveBeenCalled();
    expect(api.guardar).not.toHaveBeenCalled();
  });
});
