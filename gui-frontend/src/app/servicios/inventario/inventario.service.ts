import { BusquedaProveedor } from '../../models/proveedores/proveedor.model';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../http/api.service';
import { Row } from '../../models/shared.model';
@Injectable({ providedIn: 'root' })
export class InventarioService {
  constructor(private api: ApiService) {}
  cambiarNombre(id: number, name: string, expectedName: string): Observable<Row> {
    return this.api.post('/assets/' + id + '/name', { name, expectedName });
  }
  registrar(body: Row): Observable<Row[]> {
    return this.api.post('/inventory/registrations', body);
  }
  buscar(q: string): Observable<Row> {
    return this.api.get('/assets/search?q=' + encodeURIComponent(q.trim()));
  }
  qr(token: string): Observable<Row> {
    return this.api.get('/assets/qr/' + encodeURIComponent(token));
  }
  historial(id: number): Observable<Row[]> {
    return this.api.get('/assets/' + id + '/history');
  }
  especificaciones(id: number): Observable<Row[]> {
    return this.api.get('/assets/' + id + '/specifications');
  }
  proveedor(document: string): Observable<BusquedaProveedor> {
    return this.api.get('/institutions/search?document=' + encodeURIComponent(document.trim()));
  }
}
