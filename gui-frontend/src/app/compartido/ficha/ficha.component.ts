import { Component, Input, Output, EventEmitter, OnChanges } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { InventarioService } from '../../servicios/inventario/inventario.service';
import { InicioSesionService } from '../../servicios/usuarios/login/iniciar-sesion.service';
import { Row } from '../../models/shared.model';
@Component({
  selector: 'gui-ficha',
  templateUrl: './ficha.component.html',
  styles: [':host { display: contents; }'],
})
export class FichaComponent implements OnChanges {
  @Input() asset: Row = {};
  @Output() close = new EventEmitter<void>();
  @Output() photosChanged = new EventEmitter<void>();
  @Output() qr = new EventEmitter<Row>();
  tab = 'history';
  events: Row[] = [];
  specs: Row[] = [];
  busy = false;
  error = '';
  private generation = 0;
  constructor(private api: InventarioService, public auth: InicioSesionService) {}
  async ngOnChanges(): Promise<void> {
    const version = ++this.generation;
    this.busy = true;
    this.error = '';
    this.events = [];
    this.specs = [];
    try {
      const [events, specs] = await Promise.all([
        firstValueFrom(this.api.historial(this.asset.id)),
        firstValueFrom(this.api.especificaciones(this.asset.id)),
      ]);
      if (version === this.generation) {
        this.events = events;
        this.specs = specs;
      }
    } catch (error) {
      if (version === this.generation) this.error = (error as Error).message;
    } finally {
      if (version === this.generation) this.busy = false;
    }
  }
  get details(): any[][] {
    return [
      ['ID del activo', this.asset.id],
      ['Código patrimonial', this.asset.code],
      ['Número de serie', this.asset.serial],
      ['Tipo de producto', this.asset.family],
      ['Categoría', this.asset.category],
      ['Marca', this.asset.brand],
      ['Modelo', this.asset.model],
    ];
  }
}
