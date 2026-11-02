import { Component, OnInit, OnDestroy, Input, Output, EventEmitter } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { Row } from '../../models/shared.model';

export function qrSearchText(value: unknown): string {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('es')
    .trim();
}
interface AssetGroup {
  key: string;
  name: string;
  brand: string;
  model: string;
  location: string;
  rows: Row[];
}
@Component({
  selector: 'gui-product-selector, gui-qr-selector',
  templateUrl: './qr-selector.component.html',
  styleUrls: ['./qr-selector.component.scss'],
})
export class QrSelectorComponent implements OnInit, OnDestroy {
  @Input() mode: 'print' | 'asset' | 'model' = 'print';
  @Input() purpose = 'view';
  @Input() providedRows: Row[] = [];
  @Input() initialSelection: Row[] = [];
  @Output() pickedMany = new EventEmitter<Row[]>();
  ngOnChanges(): void {
    if (!this.multiple && this.selection.size > 1) this.selection.clear();
  }
  private roomFor(rows: Row[]): boolean {
    if (
      this.mode === 'asset' &&
      this.purpose === 'movement' &&
      new Set([...this.selection.keys(), ...rows.map((row) => row.id)]).size > 100
    ) {
      this.scanError = 'Máximo 100 activos por lote de movimiento.';
      return false;
    }
    return true;
  }
  get multiple(): boolean {
    return this.mode === 'print' || (this.mode === 'asset' && this.purpose === 'movement');
  }
  scanInput = '';
  cameraOpen = false;
  scanMessage = '';
  scanError = '';
  addScanned(event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.scanValue(this.scanInput);
    this.scanInput = '';
  }
  scanValue(raw: string): void {
    this.scanMessage = '';
    this.scanError = '';
    if (this.loading || this.error) {
      this.scanError = 'Espera a que termine de cargar el inventario.';
      return;
    }
    let value = raw.trim();
    if (value.includes('/a/')) value = value.slice(value.lastIndexOf('/a/') + 3).split(/[?#/]/)[0];
    const match = this.rows.find((row) =>
      [row.code, row.token, String(row.id)].some(
        (key) => key != null && String(key).toUpperCase() === value.toUpperCase()
      )
    );
    if (!value || !match) {
      this.scanError = 'El QR o código no corresponde a un activo disponible dentro de tu alcance.';
      return;
    }
    if (this.selection.has(match.id)) {
      this.scanMessage = match.code + ' ya está en la selección.';
      return;
    }
    if (this.purpose === 'movement' && this.selection.size >= 100) {
      this.scanError = 'Máximo 100 activos por lote de movimiento.';
      return;
    }
    if (!this.multiple) this.selection.clear();
    this.selection.set(match.id, match);
    this.scanMessage = match.code + ' añadido.';
  }
  @Output() picked = new EventEmitter<Row>();
  @Output() back = new EventEmitter<void>();
  get heading(): string {
    if (this.mode === 'print') return '¿Qué etiquetas necesitas?';
    if (this.mode === 'model') return 'Reutilizar un modelo de producto';
    return (
      (
        {
          view: 'Consultar un activo',
          edit: 'Elegir el activo para editar su nombre',
          movement: 'Elegir activos para el movimiento',
          maintenance: 'Elegir el activo para mantenimiento',
          qr: 'Elegir una etiqueta QR',
        } as Record<string, string>
      )[this.purpose] || 'Elegir un activo'
    );
  }
  get help(): string {
    if (this.mode === 'print')
      return 'Busca un producto y añade todas sus unidades o elige códigos específicos.';
    if (this.mode === 'model')
      return 'Elige una referencia del catálogo. La cantidad, las series y los nuevos códigos se registran después.';
    if (this.purpose === 'movement')
      return 'Añade unidades buscando o escaneando QR. El tipo, destino y motivo se aplicarán al lote; cada unidad tendrá su propio movimiento.';
    return 'Busca el producto, despliega sus unidades y selecciona un código patrimonial. Se elige una sola unidad.';
  }
  get confirmLabel(): string {
    if (this.mode === 'print') return 'Ver etiquetas';
    if (this.mode === 'model') return 'Usar modelo';
    return (
      (
        {
          view: 'Ver ficha',
          edit: 'Editar nombre',
          movement: 'Usar en movimiento',
          maintenance: 'Usar en mantenimiento',
          qr: 'Ver QR',
        } as Record<string, string>
      )[this.purpose] || 'Usar activo'
    );
  }
  rows: Row[] = [];
  selection = new Map<number, Row>();
  query = '';
  family = '';
  location = '';
  loading = false;
  error = '';
  preview: Row[] = [];
  expanded = new Set<string>();
  limit = 20;
  private generation = 0;
  constructor(private catalogs: CatalogosService) {}
  ngOnInit(): void {
    this.selection = new Map(this.initialSelection.map((row) => [row.id, row]));
    if (this.mode === 'model') this.rows = [...this.providedRows];
    else void this.load();
  }
  ngOnDestroy(): void {
    this.generation++;
  }
  async load(): Promise<void> {
    const generation = ++this.generation;
    this.loading = true;
    this.error = '';
    this.rows = [];
    const collected = new Map<number, Row>();
    try {
      for (let index = 0; ; index++) {
        const page = await firstValueFrom(this.catalogs.pagina(30, index));
        if (generation !== this.generation) return;
        for (const row of page.rows) collected.set(row.id, row);
        if (!page.hasMore) break;
        if (!page.rows.length) throw Error('No se pudo completar la búsqueda. Inténtalo de nuevo.');
      }
      this.rows = Array.from(collected.values());
      this.selection = new Map(
        Array.from(this.selection.keys())
          .filter((id) => collected.has(id))
          .map((id) => [id, collected.get(id)!])
      );
    } catch (error) {
      if (generation === this.generation) this.error = (error as Error).message;
    } finally {
      if (generation === this.generation) this.loading = false;
    }
  }
  get families(): string[] {
    return Array.from(new Set(this.rows.map((r) => String(r.family || '')).filter(Boolean))).sort();
  }
  get locations(): string[] {
    return Array.from(
      new Set(this.rows.map((r) => String(r.location || '')).filter(Boolean))
    ).sort();
  }
  get matches(): Row[] {
    const terms = qrSearchText(this.query).split(/\s+/).filter(Boolean);
    return this.rows.filter(
      (row) =>
        (!this.family || row.family === this.family) &&
        (!this.location || row.location === this.location) &&
        terms.every((term) =>
          qrSearchText(
            [row.code, row.name, row.brand, row.model, row.serial, row.location, row.family].join(
              ' '
            )
          ).includes(term)
        )
    );
  }
  get groups(): AssetGroup[] {
    const groups = new Map<string, AssetGroup>();
    for (const row of this.matches) {
      const key =
        this.mode === 'model'
          ? String(row.id)
          : JSON.stringify([
              row.productId,
              row.name,
              row.brand,
              row.model,
              row.locationId ?? row.location,
            ]);
      if (!groups.has(key))
        groups.set(key, {
          key,
          name: row.name,
          brand: row.brand,
          model: row.model,
          location: row.location,
          rows: [],
        });
      groups.get(key)!.rows.push(row);
    }
    return Array.from(groups.values());
  }
  get chosen(): Row[] {
    return Array.from(this.selection.values()).sort((a, b) =>
      String(a.code).localeCompare(String(b.code))
    );
  }
  hasGroup(group: AssetGroup): boolean {
    return group.rows.every((row) => this.selection.has(row.id));
  }
  toggle(row: Row): void {
    if (!this.multiple) {
      const remove = this.selection.has(row.id);
      this.selection.clear();
      if (!remove) this.selection.set(row.id, row);
      return;
    }
    if (this.selection.has(row.id)) this.selection.delete(row.id);
    else if (this.roomFor([row])) this.selection.set(row.id, row);
  }
  toggleGroup(group: AssetGroup): void {
    if (!this.multiple) {
      if (group.rows.length === 1) this.toggle(group.rows[0]);
      else this.expanded.add(group.key);
      return;
    }
    const remove = this.hasGroup(group);
    if (!remove && !this.roomFor(group.rows)) return;
    for (const row of group.rows) {
      if (remove) this.selection.delete(row.id);
      else if (this.roomFor([row])) this.selection.set(row.id, row);
    }
  }
  addMatches(): void {
    if (!this.multiple || !this.roomFor(this.matches)) return;
    for (const row of this.matches) this.selection.set(row.id, row);
  }
  expand(key: string): void {
    if (this.expanded.has(key)) this.expanded.delete(key);
    else this.expanded.add(key);
  }
  resetFilters(): void {
    this.query = '';
    this.family = '';
    this.location = '';
    this.limit = 20;
  }
  showPreview(): void {
    if (this.loading || this.error || !this.selection.size) return;
    if (this.mode === 'print') this.preview = this.chosen;
    else if (this.multiple) {
      if (this.selection.size > 100) {
        this.scanError = 'Máximo 100 activos por lote de movimiento.';
        return;
      }
      this.pickedMany.emit(this.chosen);
    } else if (this.selection.size === 1) this.picked.emit(this.chosen[0]);
  }
  trackGroup(_index: number, group: AssetGroup): string {
    return group.key;
  }
  trackAsset(_index: number, row: Row): number {
    return row.id;
  }
}
