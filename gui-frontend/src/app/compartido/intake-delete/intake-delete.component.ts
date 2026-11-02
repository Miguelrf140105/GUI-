import { Component, Input, Output, EventEmitter } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Row } from '../../models/shared.model';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { InicioSesionService } from '../../servicios/usuarios/login/iniciar-sesion.service';
@Component({
  selector: 'gui-intake-delete',
  templateUrl: './intake-delete.component.html',
  styles: [
    ':host { display: contents; }',
    '.intake-delete-warning { display: block; margin-bottom: 20px; }',
    '.intake-delete-warning strong { display: block; margin-bottom: 8px; }',
    '.intake-delete-warning p { margin: 0; line-height: 1.6; }',
  ],
})
export class IntakeDeleteComponent {
  @Input() row: Row = {};
  @Output() busyChange = new EventEmitter<boolean>();
  @Output() cancel = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();
  detail: { lines: Row[]; documents: Row[]; units?: Row[] } | null = null;
  busy = false;
  error = '';
  constructor(private api: CatalogosService, public auth: InicioSesionService) {}
  get total(): number {
    return this.detail?.units?.length || 0;
  }
  // Impide confirmar una anulación parcial; SQL vuelve a validar bajo bloqueo.
  get blockReason(): string {
    if (!this.detail) return 'Cargando y verificando todas las unidades del ingreso…';
    if (!this.detail.units)
      return 'No se pudo verificar la lista de unidades. Cierra esta ventana e inténtalo nuevamente.';
    const blocked = this.detail.units.find((unit) => unit.blockReason);
    if (blocked) return blocked.blockReason;
    const expected = this.detail.lines.reduce((n, line) => n + Number(line.quantity), 0);
    if (expected !== this.total)
      return 'No se muestran todas las unidades del ingreso o hay unidades pendientes de registrar; no se puede eliminar.';
    return '';
  }
  async submit(): Promise<void> {
    if (this.busy || this.blockReason || !this.auth.can(1403)) return;
    this.busy = true;
    this.error = '';
    this.busyChange.emit(true);
    try {
      await firstValueFrom(this.api.guardar('/intakes/' + this.row.id + '/delete', {}));
      this.saved.emit();
    } catch (error) {
      this.error = (error as Error).message;
    } finally {
      this.busy = false;
      this.busyChange.emit(false);
    }
  }
}
