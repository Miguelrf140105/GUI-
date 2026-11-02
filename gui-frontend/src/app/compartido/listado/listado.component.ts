import { Component, Input, OnInit, OnDestroy } from '@angular/core';

import { firstValueFrom, Subscription } from 'rxjs';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { AplicacionService } from '../../servicios/compartido/aplicacion.service';
import { InicioSesionService } from '../../servicios/usuarios/login/iniciar-sesion.service';
import { InventarioService } from '../../servicios/inventario/inventario.service';
import { modules } from '../../models/modulos.model';
import { Row, Page } from '../../models/shared.model';
import { createExcelWorkbook } from './excel-export';

@Component({
  selector: 'gui-listado',
  templateUrl: './listado.component.html',
  styles: [
    ':host { display: contents; }',
    '.location-delete-copy { display: grid; gap: 14px; }',
    '.location-delete-copy h3, .location-delete-copy p { margin: 0; line-height: 1.6; }',
  ],
})
export class ListadoComponent implements OnInit, OnDestroy {
  @Input() moduleId = 30;
  result: Page = { rows: [], page: 0, size: 50, hasMore: false };
  locationTree: Row[] = [];
  index = 0;
  query = '';
  filter = '';
  egressType = '';
  dateFrom = '';
  dateTo = '';
  columnQueries: Record<string, string> = {};
  get hasColumnFilters(): boolean {
    return Object.values(this.columnQueries).some((value) => value.trim());
  }
  clearInventoryFilters(): void {
    this.columnQueries = {};
    this.query = '';
    this.filter = '';
  }
  private searchable(value: unknown): string {
    return String(value ?? '—')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLocaleLowerCase('es')
      .trim();
  }
  assetSearch = '';
  scannerOpen = false;
  actionPickerOpen = false;
  pickerAction = 'view';
  chooseManyForAction(rows: Row[]): void {
    if (this.pickerAction === 'movement' && this.auth.can(402) && rows.length) {
      this.actionPickerOpen = false;
      this.modal = { kind: 'movement', assets: rows };
    }
  }
  chooseForAction(row: Row): void {
    this.actionPickerOpen = false;
    if (this.pickerAction === 'edit' && this.auth.can(302) && this.auth.can(504))
      this.open('asset-name', row);
    else if (this.pickerAction === 'qr') this.open('qr', row);
    else if (this.pickerAction === 'movement' && this.auth.can(402))
      this.modal = { kind: 'movement', asset: row };
    else if (this.pickerAction === 'maintenance' && this.auth.can(602))
      this.modal = { kind: 'maintenance', asset: row };
    else this.selected = row;
  }
  qrSelectorOpen = false;
  printAssets: Row[] = [];
  assetDelete: Row | null = null;
  assetDeleteBusy = false;
  assetDeleteError = '';
  requestAssetDelete(row: Row): void {
    if (this.moduleId !== 30 || !this.auth.can(304)) return;
    this.assetDelete = row;
    this.assetDeleteError = '';
  }
  async deleteAsset(): Promise<void> {
    if (!this.assetDelete || this.assetDeleteBusy || !this.auth.can(304)) return;
    this.assetDeleteBusy = true;
    this.assetDeleteError = '';
    try {
      await firstValueFrom(this.api.guardar('/assets/' + this.assetDelete.id + '/delete', {}));
      this.assetDelete = null;
      await this.app.refresh('Registro eliminado correctamente.');
    } catch (e) {
      this.assetDeleteError = (e as Error).message;
    } finally {
      this.assetDeleteBusy = false;
    }
  }
  providerDelete: Row | null = null;
  providerDeleteBusy = false;
  providerDeleteError = '';
  canDeleteProvider(row: Row): boolean {
    return this.moduleId === 130 && this.auth.can(1303) && row.stateId !== 2;
  }
  requestProviderDelete(row: Row): void {
    if (!this.canDeleteProvider(row)) return;
    this.providerDelete = row;
    this.providerDeleteError = '';
  }
  async deleteProvider(): Promise<void> {
    const row = this.providerDelete;
    if (!row || this.providerDeleteBusy || !this.canDeleteProvider(row)) return;
    this.providerDeleteBusy = true;
    this.providerDeleteError = '';
    try {
      await firstValueFrom(this.api.guardar('/institutions/' + row.id + '/delete', {}));
      this.providerDelete = null;
      this.index = 0;
      await this.app.refresh(
        'Proveedor eliminado. Sus ingresos, documentos e historial se conservan.'
      );
    } catch (error) {
      this.providerDeleteError = (error as Error).message;
    } finally {
      this.providerDeleteBusy = false;
    }
  }
  locationDelete: Row | null = null;
  locationDeleteBusy = false;
  locationDeleteError = '';
  requestLocationDelete(row: Row): void {
    if (this.moduleId !== 50 || !this.auth.can(503) || row.stateId === 2) return;
    this.locationDelete = this.locationTree.find((item) => item.id === row.id) || row;
    this.locationDeleteError = '';
  }
  get locationDeleteBlock(): string {
    const row = this.locationDelete;
    if (!row) return '';
    if (this.locationTree.some((item) => item.parentId === row.id && item.stateId !== 2))
      return 'Esta ubicación contiene otros espacios. Primero debes gestionar sus ubicaciones hijas.';
    if (Number(row.quantity) > 0)
      return 'Esta ubicación tiene activos asignados. Primero debes trasladarlos.';
    return '';
  }
  async deleteLocation(): Promise<void> {
    const row = this.locationDelete;
    if (!row || this.locationDeleteBusy || this.locationDeleteBlock || !this.auth.can(503)) return;
    this.locationDeleteBusy = true;
    this.locationDeleteError = '';
    try {
      await firstValueFrom(this.api.guardar('/locations/' + row.id + '/delete', {}));
      if (this.locationFilterId === row.id) this.locationFilterId = null;
      this.locationDelete = null;
      await this.app.refresh('Ubicación eliminada. Su historial se conserva.');
    } catch (error) {
      this.locationDeleteError = (error as Error).message;
    } finally {
      this.locationDeleteBusy = false;
    }
  }
  locationFilterId: number | null = null;
  loading = false;
  exporting = false;
  error = '';
  modal: { kind: string; row?: Row; asset?: Row; assets?: Row[]; parentId?: number | null } | null =
    null;
  selected: Row | null = null;
  modalBusy = false;
  private refreshSubscription?: Subscription;
  private generation = 0;
  private searchGeneration = 0;
  private transitioning = new Set<number>();
  createKind: Record<number, string> = {
    30: 'inventory',
    40: 'movement',
    50: 'location',
    60: 'maintenance',
    70: 'document',
    90: 'user',
    100: 'role',
    110: 'parameter',
    130: 'institution',
  };
  permissionWrite: Record<number, number> = {
    30: 302,
    40: 402,
    50: 502,
    60: 602,
    70: 702,
    90: 902,
    100: 1002,
    110: 1102,
    130: 1302,
  };
  constructor(
    public app: AplicacionService,
    public auth: InicioSesionService,
    private api: CatalogosService,
    private inventory: InventarioService
  ) {}
  ngOnInit(): void {
    this.refreshSubscription = this.app.refresh$.subscribe(() => this.load());
  }
  ngOnDestroy(): void {
    this.generation++;
    this.searchGeneration++;
    this.refreshSubscription?.unsubscribe();
  }
  get current(): any {
    return modules.find((module) => module.id === this.moduleId);
  }
  get columns(): any[] {
    return this.moduleId === 30
      ? this.current.columns.filter((column: any) => column.key !== 'id')
      : this.current.columns;
  }
  get writable(): boolean {
    return (
      this.auth.can(this.permissionWrite[this.moduleId]) &&
      (this.moduleId !== 30 || this.auth.can(1402))
    );
  }
  get filtered(): Row[] {
    return this.result.rows.filter(
      (row) =>
        Object.values(row).some((value) =>
          String(value ?? '')
            .toLocaleLowerCase('es')
            .includes(this.query.toLocaleLowerCase('es'))
        ) &&
        (this.moduleId !== 150 ||
          ((!this.egressType || String(row.typeId) === this.egressType) &&
            (!this.dateFrom || String(row.date).slice(0, 10) >= this.dateFrom) &&
            (!this.dateTo || String(row.date).slice(0, 10) <= this.dateTo))) &&
        (!this.filter ||
          String(this.moduleId === 110 ? row.groupId : row.stateId) === this.filter) &&
        (this.moduleId !== 30 ||
          this.columns.every((column) =>
            this.searchable(row[column.key]).includes(
              this.searchable(this.columnQueries[column.key] || '')
            )
          ))
    );
  }
  get filters(): any[] {
    return this.moduleId === 110
      ? [
          { id: 1, name: 'Categorías' },
          { id: 2, name: 'Tipos de espacio' },
          { id: 3, name: 'Documentos' },
        ]
      : Array.from(new Map(this.result.rows.map((row) => [row.stateId, row.status])).entries()).map(
          ([id, name]) => ({ id, name })
        );
  }
  async load(): Promise<void> {
    const version = ++this.generation;
    this.loading = true;
    this.error = '';
    try {
      const [result, locations] = await Promise.all([
        firstValueFrom(this.api.pagina(this.moduleId, this.index)),
        this.moduleId === 50 ? firstValueFrom(this.api.arbolUbicaciones()) : Promise.resolve([]),
      ]);
      if (version === this.generation) {
        this.result = result;
        this.locationTree = locations;
      }
    } catch (error) {
      if (version === this.generation) this.error = (error as Error).message;
    } finally {
      if (version === this.generation) this.loading = false;
    }
  }
  page(index: number): void {
    this.index = index;
    this.load();
  }
  open(kind: string, row?: Row): void {
    if (kind === 'intake-delete' && !this.auth.can(1403)) return;
    if (
      (kind === 'asset-name' ||
        (row && ['location', 'user', 'role', 'institution'].includes(kind))) &&
      !this.auth.can(504)
    )
      return;
    this.modal = {
      kind,
      row,
      parentId: kind === 'location' && !row ? this.locationFilterId : null,
    };
  }
  newLocationUnder(row: Row): void {
    this.modal = { kind: 'location', parentId: row.id };
  }
  get modalTitle(): string {
    if (!this.modal) return '';
    if (this.modal.kind === 'intake-delete') return 'Eliminar ingreso y sus unidades';
    if (this.modal.kind === 'asset-name') return 'Editar nombre del producto';
    if (this.modal.kind === 'qr') return 'Etiqueta del activo';
    if (this.modal.kind === 'files') return 'Archivos del documento';
    if (this.modal.kind === 'egress-detail') return 'Detalle del egreso';
    if (this.modal.kind === 'intake-detail') return 'Detalle del ingreso';
    const titles: Record<string, string> = {
      inventory: 'bienes al inventario',
      family: 'tipo de producto',
      movement: 'movimiento',
      location: 'ubicación',
      maintenance: 'orden de mantenimiento',
      document: 'documento',
      institution: 'proveedor / institución',
      user: 'usuario',
      role: 'rol',
      parameter: 'parámetro',
    };
    return (
      (this.modal.row ? 'Editar ' : 'Registrar ') + (titles[this.modal.kind] || this.modal.kind)
    );
  }
  close(): void {
    if (!this.modalBusy) this.modal = null;
  }
  async saved(rows?: Row[]): Promise<void> {
    this.modal = null;
    if (rows?.length) this.printAssets = rows;
    try {
      await this.auth.refresh();
      await this.app.refresh();
    } catch (error) {
      this.error = (error as Error).message;
    }
  }
  async findAsset(): Promise<void> {
    const version = ++this.searchGeneration;
    this.selected = null;
    try {
      this.error = '';
      const asset = await firstValueFrom(this.inventory.buscar(this.assetSearch));
      if (version === this.searchGeneration) this.selected = asset;
    } catch (error) {
      if (version === this.searchGeneration) this.error = (error as Error).message;
    }
  }
  scanned(value: string): void {
    this.scannerOpen = false;
    this.assetSearch = value;
    void this.findAsset();
  }
  get visibleLocationTree(): Row[] {
    if (this.locationFilterId === null) return this.locationTree;
    const ids = new Set([this.locationFilterId]);
    let changed = true;
    while (changed) {
      changed = false;
      for (const row of this.locationTree) {
        if (ids.has(row.parentId) && !ids.has(row.id)) {
          ids.add(row.id);
          changed = true;
        }
      }
    }
    return this.locationTree
      .filter((row) => ids.has(row.id))
      .map((row) => (row.id === this.locationFilterId ? { ...row, parentId: null } : row));
  }
  canEdit(row: Row): boolean {
    return (
      [50, 90, 100, 130].includes(this.moduleId) &&
      this.auth.can(504) &&
      this.writable &&
      !(this.moduleId === 100 && row.id === 1)
    );
  }
  transitionBusy(id: number): boolean {
    return this.transitioning.has(id);
  }
  async transition(row: Row, action: string): Promise<void> {
    if (this.transitioning.has(row.id)) return;
    this.transitioning.add(row.id);
    try {
      await firstValueFrom(this.api.guardar('/movements/' + action, { movementId: row.id }));
      await this.app.refresh('Movimiento actualizado y registrado en el Kardex.');
    } catch (error) {
      this.error = (error as Error).message;
    } finally {
      this.transitioning.delete(row.id);
    }
  }
  async exportRows(): Promise<void> {
    if (this.exporting || this.loading || !this.filtered.length) return;
    const rows = this.filtered.map((row) => ({ ...row }));
    const columns = this.columns.map((column) => ({ ...column }));
    const title = this.current.label,
      page = this.index + 1,
      moduleId = this.moduleId;
    this.exporting = true;
    this.error = '';
    try {
      const workbook = await createExcelWorkbook(rows, columns, title, moduleId);
      const buffer = await workbook.xlsx.writeBuffer();
      const blob = new Blob([buffer], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const url = URL.createObjectURL(blob),
        link = document.createElement('a');
      try {
        link.href = url;
        link.download = 'GUI-' + title + '-pagina-' + page + '.xlsx';
        document.body.appendChild(link);
        link.click();
      } finally {
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      }
    } catch (error) {
      this.error = 'No se pudo exportar a Excel. ' + (error as Error).message;
    } finally {
      this.exporting = false;
    }
  }
}
