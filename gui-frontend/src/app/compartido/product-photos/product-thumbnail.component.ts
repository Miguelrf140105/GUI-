import { Component, Input, OnChanges } from '@angular/core';
import { ProductPhotosService } from '../../servicios/inventario/product-photos.service';
@Component({
  selector: 'gui-product-thumbnail',
  templateUrl: './product-thumbnail.component.html',
  styleUrls: ['./product-thumbnail.component.scss'],
})
export class ProductThumbnailComponent implements OnChanges {
  @Input() photoId: string | null = null;
  @Input() label = 'Foto del producto';
  failed = false;
  constructor(public photos: ProductPhotosService) {}
  ngOnChanges() {
    this.failed = false;
  }
}
