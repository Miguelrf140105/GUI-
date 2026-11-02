import { Component, Input, OnInit, Output, EventEmitter } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { Row } from '../../models/shared.model';
@Component({
  selector: 'gui-ingreso-detalle',
  templateUrl: './ingreso-detalle.component.html',
  styles: [':host { display: contents; }'],
})
export class IngresoDetalleComponent implements OnInit {
  @Input() row: Row = {};
  @Output() loaded = new EventEmitter<{ lines: Row[]; documents: Row[]; units?: Row[] }>();
  detail: { lines: Row[]; documents: Row[]; units?: Row[] } | null = null;
  get code(): string {
    return this.row.code || 'ING' + String(this.row.id).padStart(4, '0');
  }
  error = '';
  constructor(private api: CatalogosService) {}
  async ngOnInit(): Promise<void> {
    try {
      this.detail = await firstValueFrom(this.api.ingreso(this.row.id));
      this.loaded.emit(this.detail);
    } catch (error) {
      this.error = (error as Error).message;
    }
  }
}
