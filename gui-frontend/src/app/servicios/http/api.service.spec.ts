import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApiService } from './api.service';
describe('Transporte HTTP Angular', () => {
  let api: ApiService, http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('descarta CSRF de la sesión expirada antes de otra escritura', () => {
    api.token().subscribe();
    http.expectOne('/api/auth/csrf').flush({ headerName: 'X-CSRF-TOKEN', token: 'expired' });
    api.get('/auth/me').subscribe({ error: () => {} });
    http.expectOne('/api/auth/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    api.post('/locations', {}).subscribe();
    http.expectOne('/api/auth/csrf').flush({ headerName: 'X-CSRF-TOKEN', token: 'new' });
    const save = http.expectOne('/api/locations');
    expect(save.request.headers.get('X-CSRF-TOKEN')).toBe('new');
    save.flush({ id: 1 });
  });
  it('solicita CSRF antes de enviar una mutación y conserva cookies', () => {
    api.post('/locations', { id: 1, parentId: null }).subscribe();
    const token = http.expectOne('/api/auth/csrf');
    expect(token.request.withCredentials).toBeTrue();
    token.flush({ headerName: 'X-CSRF-TOKEN', token: 'csrf-local' });
    const save = http.expectOne('/api/locations');
    expect(save.request.method).toBe('POST');
    expect(save.request.headers.get('X-CSRF-TOKEN')).toBe('csrf-local');
    expect(save.request.body.parentId).toBeNull();
    expect(save.request.withCredentials).toBeTrue();
    save.flush({ id: 1 });
  });
  it('renueva el token después de un rechazo de permisos', () => {
    api.post('/locations', {}).subscribe({ error: () => {} });
    http.expectOne('/api/auth/csrf').flush({ headerName: 'X-CSRF-TOKEN', token: 'old' });
    http.expectOne('/api/locations').flush({}, { status: 403, statusText: 'Forbidden' });
    api.post('/locations', {}).subscribe();
    http.expectOne('/api/auth/csrf').flush({ headerName: 'X-CSRF-TOKEN', token: 'new' });
    const save = http.expectOne('/api/locations');
    expect(save.request.headers.get('X-CSRF-TOKEN')).toBe('new');
    save.flush({ id: 1 });
  });
});
