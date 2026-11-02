import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { NgForm } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { AplicacionService } from '../../servicios/compartido/aplicacion.service';
import { Row } from '../../models/shared.model';
import { editorValue } from '../editor-values.util';
import { validParents, locationPath } from '../ubicaciones.util';
interface Field {
  key: string;
  label: string;
  type?: string;
  options?: Row[];
  required?: boolean;
  wide?: boolean;
  max?: number;
  readonly?: boolean;
}
@Component({
  selector: 'gui-editor',
  templateUrl: './editor.component.html',
  styles: [':host { display: contents; }'],
})
export class EditorComponent implements OnInit {
  @Input() kind = '';
  @Input() row?: Row;
  @Input() initialAsset?: Row;
  @Input() initialAssets: Row[] = [];
  chosenAssets: Row[] = [];
  chooseAssets(rows: Row[]): void {
    this.chosenAssets = Array.from(new Map(rows.map((row) => [row.id, row])).values());
    this.chosenAsset = this.chosenAssets[0] || null;
    this.form.assetId = this.chosenAsset?.id ?? null;
    this.assetPickerOpen = false;
  }
  removeAsset(id: number): void {
    this.chooseAssets(this.chosenAssets.filter((row) => row.id !== id));
  }
  chosenAsset: Row | null = null;
  assetPickerOpen = false;
  chooseAsset(row: Row): void {
    this.chooseAssets([row]);
  }
  @Output() saved = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
  @Output() busyChange = new EventEmitter<boolean>();
  form: Row = {};
  busy = false;
  loading = true;
  ready = false;
  error = '';
  scopeLocationId: number | null = null;
  retry = { body: '', key: crypto.randomUUID() };
  private fieldCache?: {
    kind: string;
    row: Row | undefined;
    lookups: unknown;
    typeId: unknown;
    fields: Field[];
  };
  constructor(public app: AplicacionService, private api: CatalogosService) {}
  async ngOnInit(): Promise<void> {
    this.form = {
      ...this.row,
      date: this.row?.date?.slice(0, 10) || new Date().toLocaleDateString('en-CA'),
      stateId: this.row?.stateId ?? 1,
      roleIds: [],
      locationIds: [],
      globalScope: false,
      permissionIds: [],
    };
    try {
      if (this.initialAssets.length) this.chooseAssets(this.initialAssets);
      else if (this.initialAsset) this.chooseAsset(this.initialAsset);
      if (this.kind === 'role' && this.row)
        this.form.permissionIds = (await firstValueFrom(this.api.permisos(this.row.id))).map(
          (item) => item.id
        );
      if (this.kind === 'user' && this.row) {
        const value = await firstValueFrom(this.api.asignaciones(this.row.id));
        this.form.roleIds = value.roles.map((item: Row) => item.id);
        this.form.locationIds = value.locations
          .filter((item: Row) => item.id != null)
          .map((item: Row) => item.id);
        this.form.globalScope = value.locations.some((item: Row) => item.id === null);
      }
      this.ready = true;
    } catch (error) {
      this.error = (error as Error).message;
    } finally {
      this.loading = false;
    }
  }
  get fields(): Field[] {
    const cached = this.fieldCache;
    if (
      cached &&
      cached.kind === this.kind &&
      cached.row === this.row &&
      cached.lookups === this.app.lookups &&
      cached.typeId === this.form.typeId
    )
      return cached.fields;
    const kind = this.kind,
      row = this.row,
      form = this.form,
      options = (key: string) => this.app.options(key),
      parameters = (groupId: number) =>
        options('parameters').filter((item) => item.groupId === groupId);
    const locations = options('locations');
    const locationOptions = locations;
    const movementType = options('movementTypes').find((item) => item.id === Number(form.typeId));
    let fields: Field[] = [];
    switch (kind) {
      case 'family':
        fields = [
          {
            key: 'categoryId',
            label: 'Categoría',
            type: 'select',
            options: parameters(1),
            required: true,
          },
          {
            key: 'name',
            label: 'Tipo de producto (por ejemplo, Laptop)',
            required: true,
            max: 100,
          },
        ];
        break;
      case 'location':
        fields = [
          { key: 'name', label: 'Nombre de la ubicación', required: true, max: 150 },
          { key: 'code', label: 'Código de ubicación (protegido)', readonly: true, max: 50 },
          {
            key: 'typeId',
            label: 'Tipo de espacio',
            type: 'select',
            options: parameters(2),
            required: true,
          },
          {
            key: 'parentId',
            label: 'Ubicación superior (vacío para sede raíz)',
            type: 'location',
            wide: true,
            options: validParents(row?.id, locationOptions),
          },
          {
            key: 'responsibleId',
            label: 'Responsable',
            type: 'select',
            options: options('people'),
          },
        ];
        break;
      case 'movement':
        fields = [
          {
            key: 'assetId',
            label: 'Activos del movimiento',
            type: 'asset',
            required: true,
            wide: true,
          },
          {
            key: 'typeId',
            label: 'Tipo de movimiento',
            type: 'select',
            options: options('movementTypes').filter((t) => t.mode !== 1),
            required: true,
            wide: true,
          },
          ...(movementType?.mode === 2
            ? [
                {
                  key: 'locationId',
                  label: 'Ubicación de destino',
                  type: 'location',
                  wide: true,
                  options: locationOptions,
                  required: true,
                },
              ]
            : []),
          ...(movementType?.institutionRequired
            ? [
                {
                  key: 'institutionId',
                  label: 'Institución destinataria',
                  type: 'select',
                  options: options('institutions'),
                  required: true,
                },
              ]
            : []),
          {
            key: 'reason',
            label: 'Motivo del movimiento',
            type: 'textarea',
            required: true,
            wide: true,
            max: 500,
          },
        ];
        break;
      case 'maintenance':
        fields = [
          {
            key: 'assetId',
            label: 'Activo',
            type: 'asset',
            required: true,
            wide: true,
          },
          { key: 'date', label: 'Fecha programada', type: 'date', required: true },
          {
            key: 'technicianId',
            label: 'Técnico responsable',
            type: 'select',
            options: options('people'),
            required: true,
          },
          {
            key: 'description',
            label: 'Trabajo a realizar',
            type: 'textarea',
            required: true,
            wide: true,
            max: 500,
          },
        ];
        break;
      case 'document':
        fields = [
          {
            key: 'typeId',
            label: 'Tipo de documento',
            type: 'select',
            options: parameters(3),
            required: true,
          },
          {
            key: 'institutionId',
            label: 'Emisor / institución',
            type: 'select',
            options: options('institutions'),
            required: true,
          },
          { key: 'number', label: 'Serie y número', required: true, max: 60 },
          { key: 'date', label: 'Fecha de emisión', type: 'date', required: true },
        ];
        break;
      case 'institution':
        fields = [
          { key: 'name', label: 'Nombre o razón social', required: true, max: 200, wide: true },
          { key: 'document', label: 'RUC / DNI / identificación', required: true, max: 30 },
          { key: 'contact', label: 'Persona de contacto', max: 150 },
          { key: 'email', label: 'Correo de contacto', type: 'email', max: 150 },
          { key: 'phone', label: 'Teléfono', max: 40 },
          { key: 'address', label: 'Dirección', max: 250, wide: true },
          {
            key: 'stateId',
            label: 'Estado',
            type: 'select',
            options: [
              { id: 1, name: 'Activo' },
              { id: 0, name: 'Inactivo' },
            ],
            required: true,
          },
        ];
        break;
      case 'parameter':
        fields = [
          {
            key: 'groupId',
            label: 'Grupo',
            type: 'select',
            options: [
              { id: 1, name: 'Categorías de bienes' },
              { id: 2, name: 'Tipos de espacio' },
              { id: 3, name: 'Tipos de documento' },
            ],
            required: true,
          },
          { key: 'name', label: 'Nombre visible', required: true, max: 150 },
          { key: 'code', label: 'Código único dentro del grupo', required: true, max: 50 },
        ];
        break;
      case 'role':
        fields = [{ key: 'name', label: 'Nombre del rol', required: true, wide: true, max: 80 }];
        break;
      case 'user':
        fields = [
          { key: 'name', label: 'Nombre completo', required: true, max: 150 },
          { key: 'username', label: 'Nombre de usuario', required: true, max: 80 },
          { key: 'email', label: 'Correo institucional', type: 'email', required: true, max: 150 },
          {
            key: 'password',
            label: row
              ? 'Nueva contraseña (opcional)'
              : 'Contraseña inicial (mínimo 14 caracteres)',
            type: 'password',
            required: !row,
            max: 72,
          },
          {
            key: 'stateId',
            label: 'Estado de la cuenta',
            type: 'select',
            options: [
              { id: 1, name: 'Activo' },
              { id: 0, name: 'Inactivo' },
            ],
            required: true,
          },
        ];
        break;
    }
    this.fieldCache = {
      kind,
      row,
      lookups: this.app.lookups,
      typeId: this.form.typeId,
      fields,
    };
    return fields;
  }
  get permissionGroups(): string[] {
    return Array.from(new Set(this.app.options('permissions').map((item) => item.module)));
  }
  permissions(group: string): Row[] {
    return this.app.options('permissions').filter((item) => item.module === group);
  }
  path(id: number): string {
    return locationPath(id, this.app.options('locations'));
  }
  toggle(key: string, id: number): void {
    const ids: number[] = this.form[key] || [];
    this.form[key] = ids.includes(id) ? ids.filter((value) => value !== id) : [...ids, id];
  }
  async submit(ngForm: NgForm): Promise<void> {
    if (this.busy || !this.ready) return;
    if (ngForm.invalid) {
      ngForm.control.markAllAsTouched();
      this.error = 'Completa los campos obligatorios.';
      return;
    }
    this.busy = true;
    this.busyChange.emit(true);
    this.error = '';
    try {
      const payload: Row = {};
      for (const field of this.fields)
        payload[field.key] = editorValue(this.form[field.key], field.type);
      if (this.row) payload.id = this.row.id;
      if (this.kind === 'movement') {
        if (!this.chosenAssets.length || this.chosenAssets.length > 100)
          throw Error('Seleccione entre 1 y 100 activos.');
        payload.assetIds = this.chosenAssets.map((row) => row.id).sort((a, b) => a - b);
        delete payload.assetId;
        const type = this.app
          .options('movementTypes')
          .find((item) => item.id === Number(this.form.typeId));
        payload.locationId = type?.mode === 2 ? payload.locationId : null;
        payload.institutionId = type?.institutionRequired ? payload.institutionId : null;
        const body = JSON.stringify(payload);
        if (body !== this.retry.body) this.retry = { body, key: crypto.randomUUID() };
        payload.idempotencyKey = this.retry.key;
      }
      if (this.kind === 'role') payload.permissionIds = this.form.permissionIds;
      if (this.kind === 'user') {
        payload.roleIds = this.form.roleIds;
        payload.globalScope = !!this.form.globalScope;
        payload.locationIds = payload.globalScope ? [] : this.form.locationIds;
        if (!payload.roleIds.length) throw Error('Asigna al menos un rol.');
        if (!payload.globalScope && !payload.locationIds.length)
          throw Error('Asigna una ubicación o autoriza alcance global.');
      }
      const endpoints: Record<string, string> = {
        family: '/families',
        movement: '/movements/batch',
        location: '/locations',
        maintenance: '/maintenance',
        document: '/documents',
        institution: '/institutions',
        parameter: '/parameters',
        role: '/roles',
        user: '/users',
      };
      await firstValueFrom(this.api.guardar(endpoints[this.kind], payload));
      this.saved.emit();
    } catch (error) {
      this.error = (error as Error).message;
    } finally {
      this.busy = false;
      this.busyChange.emit(false);
    }
  }
  addScope(): void {
    if (this.scopeLocationId !== null && !this.form.locationIds.includes(this.scopeLocationId))
      this.form.locationIds = [...this.form.locationIds, this.scopeLocationId];
    this.scopeLocationId = null;
  }
}
