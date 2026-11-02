import { Injectable } from '@angular/core';
import { ApiService } from '../http/api.service';
import { Row } from '../../models/shared.model';
import { environment } from '../../../environments/environment';
@Injectable({ providedIn: 'root' })
export class ProductPhotosService {
  constructor(private api: ApiService) {}
  list(product: number) {
    return this.api.get<Row[]>('/products/' + product + '/photos');
  }
  upload(product: number, file: File) {
    const form = new FormData();
    form.append('file', file);
    return this.api.post('/products/' + product + '/photos', form);
  }
  change(product: number, photo: string, action: 'cover' | 'remove') {
    return this.api.post('/products/' + product + '/photos/' + photo + '/' + action, {});
  }
  url(photo: string, thumbnail = true) {
    return (
      environment.apiRest.host +
      '/product-photos/' +
      encodeURIComponent(photo) +
      '?thumbnail=' +
      thumbnail
    );
  }
}
