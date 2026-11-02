import { Component, Input, Output, EventEmitter, OnChanges, OnDestroy } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Row } from '../../models/shared.model';
import { ProductPhotosService } from '../../servicios/inventario/product-photos.service';
import { InicioSesionService } from '../../servicios/usuarios/login/iniciar-sesion.service';
@Component({
  selector: 'gui-product-photos',
  templateUrl: './product-photos.component.html',
  styleUrls: ['./product-photos.component.scss'],
})
export class ProductPhotosComponent implements OnChanges, OnDestroy {
  @Input() productId: number | null = null;
  @Output() changed = new EventEmitter<void>();
  photos: Row[] = [];
  selected: string | null = null;
  pendingRemove: string | null = null;
  loading = false;
  busy = false;
  error = '';
  private generation = 0;
  private destroyed = false;
  constructor(public api: ProductPhotosService, public auth: InicioSesionService) {}
  ngOnChanges() {
    this.pendingRemove = null;
    this.selected = null;
    void this.load();
  }
  ngOnDestroy() {
    this.destroyed = true;
    this.generation++;
  }
  async load() {
    const generation = ++this.generation;
    this.photos = [];
    this.error = '';
    if (!this.productId) {
      this.loading = false;
      return;
    }
    this.loading = true;
    try {
      const rows = await firstValueFrom(this.api.list(this.productId));
      if (generation === this.generation) {
        this.photos = rows;
        this.selected = rows.some((p) => p.id === this.selected)
          ? this.selected
          : rows[0]?.id || null;
      }
    } catch (e) {
      if (generation === this.generation) this.error = (e as Error).message;
    } finally {
      if (generation === this.generation) this.loading = false;
    }
  }
  async upload(event: Event) {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files || []);
    if (this.busy || !this.productId || !this.auth.can(303) || !files.length) return;
    if (files.length + this.photos.length > 5) {
      this.error = 'Cada producto admite hasta cinco fotos.';
      input.value = '';
      return;
    }
    if (files.some((f) => f.size === 0 || f.size > 10 * 1024 * 1024)) {
      this.error = 'Cada foto debe pesar como máximo 10 MB.';
      input.value = '';
      return;
    }
    const product = this.productId;
    this.busy = true;
    this.error = '';
    try {
      for (const file of files) await firstValueFrom(this.api.upload(product, file));
      if (!this.destroyed && product === this.productId) {
        await this.load();
        this.changed.emit();
      }
    } catch (e) {
      if (!this.destroyed && product === this.productId) {
        await this.load();
        this.error = (e as Error).message;
        this.changed.emit();
      }
    } finally {
      this.busy = false;
      input.value = '';
    }
  }
  async change(photo: string, action: 'cover' | 'remove') {
    if (this.busy || !this.productId || !this.auth.can(303)) return;
    if (action === 'remove' && this.pendingRemove !== photo) return;
    const product = this.productId;
    this.busy = true;
    this.error = '';
    try {
      await firstValueFrom(this.api.change(product, photo, action));
      if (!this.destroyed && product === this.productId) {
        this.pendingRemove = null;
        await this.load();
        this.changed.emit();
      }
    } catch (e) {
      if (!this.destroyed) this.error = (e as Error).message;
    } finally {
      this.busy = false;
    }
  }
}
