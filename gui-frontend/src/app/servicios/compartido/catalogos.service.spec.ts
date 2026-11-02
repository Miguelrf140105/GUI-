import { firstValueFrom, of, throwError } from 'rxjs';
import { CatalogosService } from './catalogos.service';
import { ApiService } from '../http/api.service';

describe('Árbol de ubicaciones paginado', () => {
  it('incluye ambientes de páginas posteriores sin volver a pedir la primera', async () => {
    const api = jasmine.createSpyObj<ApiService>('ApiService', ['get']);
    api.get.and.returnValues(
      of({ rows: [{id: 1}, {id: 2, parentId: 1}], page: 0, size: 2, hasMore: true }),
      of({ rows: [{id: 3, parentId: 2}], page: 1, size: 2, hasMore: false })
    );
    const rows = await firstValueFrom(new CatalogosService(api).arbolUbicaciones());
    expect(rows.map(row => row.id)).toEqual([1, 2, 3]);
    expect(api.get.calls.count()).toBe(2);
    expect(api.get.calls.argsFor(1)[0]).toContain('page=1');
  });
  it('propaga un fallo intermedio en lugar de presentar un árbol incompleto', async () => {
    const api = jasmine.createSpyObj<ApiService>('ApiService', ['get']);
    api.get.and.returnValues(
      of({ rows: [{id: 1}], page: 0, size: 1, hasMore: true }),
      throwError(() => new Error('Sin acceso'))
    );
    await expectAsync(firstValueFrom(new CatalogosService(api).arbolUbicaciones())).toBeRejectedWithError('Sin acceso');
  });
});
