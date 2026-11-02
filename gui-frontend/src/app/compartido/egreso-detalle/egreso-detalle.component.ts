import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { InicioSesionService } from '../../servicios/usuarios/login/iniciar-sesion.service';
import { Row } from '../../models/shared.model';
@Component({
  selector: 'gui-egreso-detalle',
  templateUrl: './egreso-detalle.component.html',
  styles: [':host{display:contents}'],
})
export class EgresoDetalleComponent implements OnInit {
  @Input() row: Row = {};
  @Output() busyChange = new EventEmitter<boolean>();
  detail: { units: Row[]; documents: Row[] } | null = null;
  documents: Row[] = [];
  documentId: number | null = null;
  busy = false;
  error = '';
  constructor(private api: CatalogosService, public auth: InicioSesionService) {}
  async ngOnInit(): Promise<void> {
    try {
      this.detail = await firstValueFrom(this.api.egreso(this.row.id));
    } catch (e) {
      this.error = (e as Error).message;
      return;
    }
    if (this.auth.can(1502)) {
      try {
        // Carga todas las páginas permitidas para no ocultar documentos al crecer el archivo.
        let page = 0;
        let result;
        do {
          result = await firstValueFrom(this.api.pagina(70, page++));
          this.documents.push(...result.rows);
        } while (result.hasMore);
      } catch (e) {
        this.error = (e as Error).message;
      }
    }
  }
  async associate(): Promise<void> {
    if (this.busy || !this.documentId || !this.auth.can(1502)) return;
    this.busy = true;
    this.busyChange.emit(true);
    this.error = '';
    try {
      await firstValueFrom(
        this.api.guardar('/egresses/' + this.row.id + '/documents', { documentId: this.documentId })
      );
      this.detail = await firstValueFrom(this.api.egreso(this.row.id));
      this.documentId = null;
    } catch (e) {
      this.error = (e as Error).message;
    } finally {
      this.busy = false;
      this.busyChange.emit(false);
    }
  }
}
