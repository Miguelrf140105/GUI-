import { Component, Input, Output, EventEmitter, OnInit, OnChanges } from '@angular/core';
import { NgForm } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Row } from '../../models/shared.model';
import { AplicacionService } from '../../servicios/compartido/aplicacion.service';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { locationPath } from '../ubicaciones.util';

type Level = 'site' | 'floor' | 'room' | 'subroom';
@Component({
  selector: 'gui-location-create',
  templateUrl: './location-create.component.html',
  styleUrls: ['./location-create.component.scss'],
})
export class LocationCreateComponent implements OnInit, OnChanges {
  @Input() locations: Row[] = [];
  @Input() contextParentId: number | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
  @Output() busyChange = new EventEmitter<boolean>();
  level: Level = 'room';
  siteId: number | null = null;
  floorId: number | null = null;
  roomId: number | null = null;
  name = '';
  floorNumber: number | null = null;
  typeId: number | null = null;
  overrideResponsible = false;
  responsibleId: number | null = null;
  busy = false;
  error = '';
  private initialized = false;
  levels = [
    { id: 'site', name: 'Sede' },
    { id: 'floor', name: 'Piso' },
    { id: 'room', name: 'Ambiente' },
    { id: 'subroom', name: 'Subambiente' },
  ];
  constructor(public app: AplicacionService, private api: CatalogosService) {}
  ngOnInit(): void {
    this.initialize();
  }
  ngOnChanges(): void {
    this.initialize();
  }
  get active(): Row[] {
    return this.locations.filter((row) => row.stateId == null || row.stateId === 1);
  }
  get types(): Row[] {
    return this.app.options('parameters').filter((row) => row.groupId === 2);
  }
  type(code: string): number | null {
    return (
      this.types.find(
        (row) =>
          String(row.code || row.name)
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .toUpperCase() === code
      )?.id ?? null
    );
  }
  get spaceTypes(): Row[] {
    return this.types.filter(
      (row) => ![this.type('SEDE'), this.type('PISO'), this.type('PABELLON')].includes(row.id)
    );
  }
  get sites(): Row[] {
    return this.active.filter((row) => row.parentId == null);
  }
  isBelow(row: Row, parent: number): boolean {
    const seen = new Set<number>();
    let current: Row | undefined = row;
    while (current && !seen.has(current.id)) {
      if (current.id === parent) return true;
      seen.add(current.id);
      current = this.locations.find((item) => item.id === current!.parentId);
    }
    return false;
  }
  get floors(): Row[] {
    return this.active.filter(
      (row) =>
        row.typeId === this.type('PISO') && (this.siteId === null || this.isBelow(row, this.siteId))
    );
  }
  get rooms(): Row[] {
    const parent = this.floorId === 0 ? this.siteId : this.floorId;
    return this.active.filter(
      (row) =>
        ![this.type('SEDE'), this.type('PISO'), this.type('PABELLON')].includes(row.typeId) &&
        (parent === null || row.parentId === parent)
    );
  }
  get parentId(): number | null {
    return this.level === 'site'
      ? null
      : this.level === 'floor'
      ? this.siteId
      : this.level === 'room'
      ? this.floorId === 0
        ? this.siteId
        : this.floorId
      : this.roomId;
  }
  get parent(): Row | undefined {
    return this.active.find((row) => row.id === this.parentId);
  }
  get finalName(): string {
    return this.level === 'floor' ? 'Piso ' + this.floorNumber : this.name.trim();
  }
  get finalType(): number | null {
    return this.level === 'site'
      ? this.type('SEDE')
      : this.level === 'floor'
      ? this.type('PISO')
      : this.typeId;
  }
  get inheritedResponsible(): string {
    if (!this.parent) return 'Se determinará al elegir la ubicación';
    const id = this.parent?.responsibleId;
    return (
      this.app.options('people').find((row) => row.id === id)?.name ||
      this.parent?.responsible ||
      'Sin responsable asignado'
    );
  }
  get previewPath(): string {
    return [
      this.parentId === null ? '' : locationPath(this.parentId, this.locations),
      this.level === 'floor' && this.floorNumber === null
        ? 'Nuevo piso'
        : this.finalName || 'Nuevo espacio',
    ]
      .filter(Boolean)
      .join(' / ');
  }
  initialize(): void {
    if (this.initialized || !this.locations.length) return;
    this.initialized = true;
    if (this.sites.length === 1) this.siteId = this.sites[0].id;
    const context = this.active.find((row) => row.id === this.contextParentId);
    if (context) {
      this.siteId = this.sites.find((row) => this.isBelow(context, row.id))?.id ?? null;
      const floor = this.active.find(
        (row) => row.typeId === this.type('PISO') && this.isBelow(context, row.id)
      );
      this.floorId = floor?.id ?? 0;
      if (
        context.typeId !== this.type('PISO') &&
        context.parentId != null &&
        context.typeId !== this.type('PABELLON')
      ) {
        this.level = 'subroom';
        this.roomId = context.id;
      }
    }
    if (!this.floors.length && this.siteId !== null) this.floorId = 0;
    this.suggestFloor();
  }
  setLevel(value: Level): void {
    this.level = value;
    this.error = '';
    if (value === 'site') this.typeId = null;
    if (value === 'floor') this.suggestFloor();
  }
  siteChanged(): void {
    this.floorId = this.floors.length ? null : 0;
    this.roomId = null;
    this.suggestFloor();
  }
  floorChanged(): void {
    this.roomId = null;
  }
  suggestFloor(): void {
    this.floorNumber =
      Math.max(0, ...this.floors.map((row) => Number(/^Piso\s+(\d+)$/i.exec(row.name)?.[1] || 0))) +
      1;
  }
  async submit(form: NgForm): Promise<void> {
    if (this.busy) return;
    this.error = '';
    if (
      form.invalid ||
      !this.finalName ||
      this.finalName.length > 150 ||
      !this.finalType ||
      (this.level !== 'site' && !this.parent) ||
      (this.level === 'floor' &&
        (!Number.isInteger(this.floorNumber) || Number(this.floorNumber) < 1))
    ) {
      form.control.markAllAsTouched();
      this.error = 'Completa el nombre, el tipo de espacio y dónde se creará.';
      return;
    }
    this.busy = true;
    this.busyChange.emit(true);
    try {
      await firstValueFrom(
        this.api.guardar('/locations', {
          name: this.finalName,
          typeId: this.finalType,
          parentId: this.parentId,
          responsibleId: this.overrideResponsible ? this.responsibleId : null,
        })
      );
      this.saved.emit();
    } catch (error) {
      this.error = (error as Error).message;
    } finally {
      this.busy = false;
      this.busyChange.emit(false);
    }
  }
}
