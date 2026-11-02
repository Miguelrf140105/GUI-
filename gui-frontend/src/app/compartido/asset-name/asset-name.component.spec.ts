import { of, throwError } from 'rxjs';
import { AssetNameComponent } from './asset-name.component';
describe('Edición restringida del nombre de activo', () => {
  it('envía solo ID, nombre y nombre anterior, sin código ni QR', async () => {
    const service = jasmine.createSpyObj('inventario', ['cambiarNombre']);
    service.cambiarNombre.and.returnValue(of({}));
    const c = new AssetNameComponent(service);
    c.asset = { id: 2, name: 'Original', code: 'MIC-000001', token: 'qr' };
    c.ngOnInit();
    c.name = '  Nombre corregido  ';
    const saved = jasmine.createSpy('saved');
    c.saved.subscribe(saved);
    await c.submit();
    expect(service.cambiarNombre).toHaveBeenCalledOnceWith(2, 'Nombre corregido', 'Original');
    expect(c.asset.code).toBe('MIC-000001');
    expect(c.asset.token).toBe('qr');
    expect(saved).toHaveBeenCalled();
  });
  it('no envía nombres vacíos, sin cambios ni fuera del límite', async () => {
    const service = jasmine.createSpyObj('inventario', ['cambiarNombre']);
    const c = new AssetNameComponent(service);
    c.asset = { id: 2, name: 'Original' };
    c.ngOnInit();
    await c.submit();
    c.name = ' ';
    await c.submit();
    c.name = 'a'.repeat(201);
    await c.submit();
    expect(service.cambiarNombre).not.toHaveBeenCalled();
  });
  it('conserva el nombre escrito y muestra el conflicto para reintentar', async () => {
    const service = jasmine.createSpyObj('inventario', ['cambiarNombre']);
    service.cambiarNombre.and.returnValue(
      throwError(() => new Error('Nombre modificado en otra sesión'))
    );
    const c = new AssetNameComponent(service);
    c.asset = { id: 2, name: 'Original' };
    c.ngOnInit();
    c.name = 'Nuevo';
    await c.submit();
    expect(c.error).toContain('otra sesión');
    expect(c.name).toBe('Nuevo');
    expect(c.busy).toBeFalse();
  });
});
