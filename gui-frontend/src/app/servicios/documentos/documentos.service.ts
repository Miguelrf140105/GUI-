import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../http/api.service';
import { Row } from '../../models/shared.model';
import { environment } from '../../../environments/environment';
@Injectable({ providedIn: 'root' })
export class DocumentosService {
  constructor(private api: ApiService) {}
  listar(parent: { documentId?: number; assetId?: number }): Observable<Row[]> {
    const query = Object.entries(parent)
      .filter(([, value]) => value != null)
      .map(([key, value]) => key + '=' + value)
      .join('&');
    return this.api.get('/attachments?' + query);
  }
  subir(parent: { documentId?: number; assetId?: number }, file: File): Observable<any> {
    const data = new FormData();
    Object.entries(parent)
      .filter(([, value]) => value != null)
      .forEach(([key, value]) => data.append(key, String(value)));
    data.append('file', file);
    return this.api.post('/attachments', data);
  }
  url(id: number): string {
    return environment.apiRest.host + '/attachments/' + id;
  }
}
