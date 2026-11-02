import { Injectable } from '@angular/core';
import { Observable, EMPTY } from 'rxjs';
import { expand, reduce } from 'rxjs/operators';
import { ApiService } from '../http/api.service';
import { Lookups, Page, Row } from '../../models/shared.model';
@Injectable({ providedIn: 'root' })
export class CatalogosService {
  constructor(private api: ApiService) {}
  pagina(module: number, page = 0): Observable<Page> {
    return this.api.get('/modules/' + module + '?page=' + page + '&size=50');
  }
  /** El árbol necesita todas las páginas autorizadas, aunque la tabla muestre solo una. */
  arbolUbicaciones(): Observable<Row[]> {
    return this.pagina(50).pipe(
      expand((page) => (page.hasMore ? this.pagina(50, page.page + 1) : EMPTY)),
      reduce((rows: Row[], page) => rows.concat(page.rows), [] as Row[])
    );
  }
  lookups(): Observable<Lookups> {
    return this.api.get('/lookups');
  }
  guardar(path: string, body: Row): Observable<Row> {
    return this.api.post(path, body);
  }
  dashboard(): Observable<{ monthly: Row[]; categories: Row[] }> {
    return this.api.get('/dashboard');
  }
  permisos(id: number): Observable<Row[]> {
    return this.api.get('/roles/' + id + '/permissions');
  }
  asignaciones(id: number): Observable<any> {
    return this.api.get('/users/' + id + '/assignments');
  }
  egreso(id: number): Observable<{ units: Row[]; documents: Row[] }> {
    return this.api.get('/egresses/' + id + '/details');
  }
  ingreso(id: number): Observable<{ lines: Row[]; documents: Row[]; units?: Row[] }> {
    return this.api.get('/intakes/' + id + '/details');
  }
}
