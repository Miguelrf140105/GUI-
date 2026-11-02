import {
  Component,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
} from '@angular/core';
import { NgForm } from '@angular/forms';
import { Subject, Subscription, of, timer, firstValueFrom } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { AplicacionService } from '../../servicios/compartido/aplicacion.service';
import { InventarioService } from '../../servicios/inventario/inventario.service';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { InicioSesionService } from '../../servicios/usuarios/login/iniciar-sesion.service';
import { Row } from '../../models/shared.model';
import {
  ProductLine,
  blankLine,
  localDate,
  validateLines,
  changeFamily,
  selectModel,
  inventoryTotals,
} from '../../models/inventario/registro-inventario.model';

/** Captura de recepción: la reserva de códigos, los permisos y el Kardex pertenecen al backend. */
@Component({
  selector: 'gui-inventario-registro',
  templateUrl: './inventario-registro.component.html',
  styles: [':host { display: contents; }'],
})
export class InventarioRegistroComponent implements OnInit, OnDestroy {
  @Output() saved = new EventEmitter<Row[]>();
  @Output() cancel = new EventEmitter<void>();
  @Output() busyChange = new EventEmitter<boolean>();
  @ViewChild('formElement') formElement!: ElementRef<HTMLFormElement>;
  header: Row = {
    typeId: null,
    institutionId: null,
    date: localDate(),
    locationId: null,
    responsibleId: null,
    reason: '',
  };
  provider: Row = {
    name: '',
    document: '',
    contact: '',
    email: '',
    phone: '',
    address: '',
    stateId: 1,
  };
  lines: ProductLine[] = [blankLine()];
  modelPickerLineKey: string | null = null;
  modelChoices: Row[] = [];
  openModelPicker(line: ProductLine): void {
    if (!line.familyId || this.busy) return;
    this.modelChoices = this.models(line).map((row) => ({ ...row, family: this.familyName(line) }));
    this.modelPickerLineKey = line.key;
  }
  chooseModel(row: Row): void {
    const index = this.lines.findIndex((line) => line.key === this.modelPickerLineKey);
    if (index >= 0 && this.modelChoices.some((model) => model.id === row.id))
      this.modelChanged(index, row.id);
    this.modelPickerLineKey = null;
  }
  documents: Row[] = [];
  documentIds: number[] = [];
  existing: Row[] = [];
  documentPage = 0;
  moreDocuments = true;
  documentsLoading = false;
  openSections = ['products'];
  closedProducts: string[] = [];
  newProvider = false;
  matchedProvider: Row | null = null;
  providerSearch = 'idle';
  providerSearchError = '';
  busy = false;
  error = '';
  private retry = { body: '', key: crypto.randomUUID() };
  private documentSearch = new Subject<string>();
  private searchSubscription?: Subscription;
  providerFields = [
    { key: 'document', label: 'RUC / identificación', max: 30, required: true },
    { key: 'name', label: 'Nombre o razón social', max: 200, required: true },
    { key: 'contact', label: 'Contacto', max: 150, required: false },
    { key: 'email', label: 'Correo', max: 150, required: false },
    { key: 'phone', label: 'Teléfono', max: 40, required: false },
    { key: 'address', label: 'Dirección', max: 250, required: false },
  ];
  constructor(
    public app: AplicacionService,
    public auth: InicioSesionService,
    private inventory: InventarioService,
    private catalogos: CatalogosService
  ) {}
  ngOnInit(): void {
    this.newProvider = this.auth.can(1302) && !this.app.options('institutions').length;
    this.searchSubscription = this.documentSearch
      .pipe(
        // El cambio cancela de inmediato la petición anterior; la espera ocurre dentro.
        switchMap((document) =>
          !this.newProvider || !document.trim()
            ? of({ found: false, skipped: true })
            : timer(400).pipe(
                switchMap(() => this.inventory.proveedor(document)),
                catchError((error) => of({ found: false, error: error.message }))
              )
        )
      )
      .subscribe((result: any) => {
        if (result.skipped) {
          this.providerSearch = 'idle';
          return;
        }
        if (result.error) {
          this.providerSearch = 'error';
          this.providerSearchError = result.error;
          return;
        }
        this.matchedProvider = result.found ? result.institution : null;
        this.providerSearch = result.found ? 'found' : 'missing';
        if (this.matchedProvider)
          for (const key of ['name', 'contact', 'email', 'phone', 'address'])
            this.provider[key] = String(this.matchedProvider[key] ?? '');
      });
  }
  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }
  units(count: number | ''): string {
    return Number(count) === 1 ? 'unidad' : 'unidades';
  }
  trackLine(_index: number, line: ProductLine): string {
    return line.key;
  }
  trackSpecification(_index: number, field: Row): number {
    return field.id;
  }
  get totals(): { quantity: number } {
    return inventoryTotals(this.lines);
  }
  get effectiveInstitution(): number | null {
    return this.newProvider ? this.matchedProvider?.id ?? null : this.header.institutionId;
  }
  get originSummary(): string {
    return (
      (this.newProvider
        ? this.provider.name
        : this.app.options('institutions').find((row) => row.id === this.header.institutionId)
            ?.name) || 'Procedencia, fecha y proveedor o donante'
    );
  }
  get providerMessage(): string {
    return this.providerSearch === 'loading'
      ? 'Buscando RUC / identificación…'
      : this.providerSearch === 'found'
      ? this.matchedProvider?.stateId === 1
        ? 'Proveedor ya registrado. Se usarán sus datos sin crear otro registro.'
        : this.matchedProvider?.stateId === 2
        ? 'Este proveedor fue eliminado. Su identificación se conserva para proteger el historial; utiliza otro proveedor.'
        : 'Proveedor registrado, pero inactivo. Revísalo en Proveedores.'
      : this.providerSearch === 'missing'
      ? 'No está registrado. Completa sus datos para crearlo con esta recepción.'
      : this.providerSearch === 'error'
      ? 'No se pudo consultar: ' + this.providerSearchError
      : 'Ingresa primero el RUC / identificación para comprobar si ya existe.';
  }
  fieldsFor(familyId: number | ''): Row[] {
    return this.app
      .options('familySpecifications')
      .filter((row) => row.familyId === familyId)
      .map((row) => ({
        ...this.app.options('specifications').find((spec) => spec.id === row.specificationId),
        required: row.required,
      }));
  }
  models(line: ProductLine): Row[] {
    return this.app.options('products').filter((row) => row.familyId === line.familyId);
  }
  documentTypes(): Row[] {
    return this.app.options('parameters').filter((row) => row.groupId === 3);
  }
  movementTypes(): Row[] {
    return this.app.options('movementTypes').filter((row) => row.mode === 1);
  }
  familyName(line: ProductLine): string {
    return (
      this.app.options('families').find((row) => row.id === line.familyId)?.name ||
      'Tipo por seleccionar'
    );
  }
  toggle(section: string): void {
    this.openSections = this.openSections.includes(section)
      ? this.openSections.filter((key) => key !== section)
      : [...this.openSections, section];
  }
  toggleProduct(line: ProductLine): void {
    this.closedProducts = this.closedProducts.includes(line.key)
      ? this.closedProducts.filter((key) => key !== line.key)
      : [...this.closedProducts, line.key];
  }
  familyChanged(index: number, value: number | null): void {
    this.lines[index] = changeFamily(this.lines[index], value || '');
  }
  modelChanged(index: number, value: number | ''): void {
    this.lines[index] = selectModel(
      this.lines[index],
      this.app.options('products').find((row) => row.id === value)
    );
  }
  finish(line: ProductLine): boolean {
    try {
      validateLines([line]);
      for (const field of this.fieldsFor(line.familyId))
        if (field.required && !String(line.specifications[field.id] ?? '').trim())
          throw Error('Completa ' + field.name + '.');
      this.closedProducts = Array.from(new Set([...this.closedProducts, line.key]));
      this.error = '';
      return true;
    } catch (error) {
      this.error = (error as Error).message;
      return false;
    }
  }
  addProduct(): void {
    if (this.lines.some((line) => !this.finish(line))) return;
    if (this.lines.length >= 100) {
      this.error = 'Máximo 100 líneas por recepción.';
      return;
    }
    this.lines.push(blankLine());
  }
  remove(line: ProductLine): void {
    if (this.lines.length > 1) {
      this.lines = this.lines.filter((row) => row.key !== line.key);
      this.closedProducts = this.closedProducts.filter((key) => key !== line.key);
    }
  }
  addDocument(): void {
    if (this.documents.length < 50)
      this.documents.push({
        key: crypto.randomUUID(),
        typeId: null,
        number: '',
        date: localDate(),
      });
  }
  toggleProvider(): void {
    this.newProvider = !this.newProvider;
    this.documentIds = [];
    this.providerSearch = this.newProvider && this.provider.document ? 'loading' : 'idle';
    this.documentSearch.next(this.newProvider ? this.provider.document : '');
  }
  onDocument(value: string): void {
    if (this.matchedProvider)
      for (const key of ['name', 'contact', 'email', 'phone', 'address']) this.provider[key] = '';
    this.provider.document = value;
    this.matchedProvider = null;
    this.providerSearch = value.trim() ? 'loading' : 'idle';
    this.documentIds = [];
    this.documentSearch.next(value);
  }
  providerReady(): void {
    if (this.newProvider && this.providerSearch === 'loading')
      throw Error('Espera a que termine la búsqueda del RUC.');
    if (this.newProvider && this.providerSearch === 'error')
      throw Error('No se pudo comprobar el RUC. ' + this.providerSearchError);
    if (this.newProvider && this.matchedProvider && this.matchedProvider.stateId !== 1)
      throw Error(
        this.matchedProvider.stateId === 2
          ? 'El proveedor fue eliminado y no puede usarse en nuevas operaciones.'
          : 'El proveedor está inactivo. Revisa su estado antes de usarlo.'
      );
  }
  async loadDocuments(): Promise<void> {
    if (this.documentsLoading) return;
    this.documentsLoading = true;
    try {
      const page = await firstValueFrom(this.catalogos.pagina(70, this.documentPage));
      this.existing = [...this.existing, ...page.rows];
      this.moreDocuments = page.hasMore;
      this.documentPage++;
    } catch (error) {
      this.error = (error as Error).message;
    } finally {
      this.documentsLoading = false;
    }
  }
  get availableDocuments(): Row[] {
    return this.existing.filter((row) => row.institutionId === this.effectiveInstitution);
  }
  toggleDocument(id: number): void {
    this.documentIds = this.documentIds.includes(id)
      ? this.documentIds.filter((value) => value !== id)
      : [...this.documentIds, id];
  }
  continueTo(current: string, next: string): void {
    try {
      if (current === 'products') {
        validateLines(this.lines);
        if (this.lines.some((line) => !this.finish(line))) return;
      }
      if (current === 'origin') {
        this.providerReady();
        if (!this.header.typeId || !this.header.date)
          throw Error('Selecciona procedencia y fecha.');
        if (
          this.newProvider &&
          !this.matchedProvider &&
          (!this.provider.document.trim() || !this.provider.name.trim())
        )
          throw Error('Completa RUC y razón social.');
        if (!this.newProvider && !this.header.institutionId)
          throw Error('Selecciona el proveedor o donante.');
      }
      if (
        current === 'documents' &&
        this.documents.some((doc) => !doc.typeId || !doc.number.trim() || !doc.date)
      )
        throw Error('Completa tipo, serie/número y fecha del comprobante.');
      if (current === 'destination' && !this.header.locationId)
        throw Error('Selecciona la ubicación inicial.');
      this.openSections = this.openSections.filter((key) => key !== current && key !== next);
      this.openSections.push(next);
      this.error = '';
    } catch (error) {
      this.error = (error as Error).message;
    }
  }
  async submit(form: NgForm): Promise<void> {
    if (this.busy) return;
    this.error = '';
    try {
      this.providerReady();
      validateLines(this.lines);
      if (form.invalid) {
        form.control.markAllAsTouched();
        this.openSections = ['products', 'origin', 'documents', 'destination'];
        this.closedProducts = [];
        setTimeout(() => {
          const field = this.formElement.nativeElement.querySelector<HTMLElement>(
            'input.ng-invalid:not([type=hidden]),select.ng-invalid,textarea.ng-invalid,gui-search-select.ng-invalid input'
          );
          field?.focus();
        });
        throw Error('Completa los campos obligatorios marcados.');
      }
      this.busy = true;
      this.busyChange.emit(true);
      const payload: Row = {
        ...this.header,
        institutionId: this.effectiveInstitution,
        newInstitution: this.newProvider && !this.matchedProvider ? this.provider : null,
        documentIds: this.documentIds,
        documents: this.documents.map((doc) => ({
          typeId: doc.typeId,
          number: doc.number.trim(),
          date: doc.date,
        })),
        lines: this.lines.map((line) => ({
          familyId: line.familyId,
          productId: line.productId || null,
          name: line.productId ? null : line.name.trim(),
          brand: line.productId ? null : line.brand.trim(),
          model: line.productId ? null : line.model.trim(),
          quantity: Number(line.quantity),
          serials: line.serialList.trim()
            ? line.serialList.split(/\r?\n/).map((value) => value.trim())
            : [],
          specifications: this.fieldsFor(line.familyId)
            .filter((field) => String(line.specifications[field.id] ?? '').trim())
            .map((field) => ({
              id: field.id,
              value: String(line.specifications[field.id]).trim(),
            })),
        })),
      };
      const body = JSON.stringify(payload);
      if (body !== this.retry.body) this.retry = { body, key: crypto.randomUUID() };
      payload.idempotencyKey = this.retry.key;
      const rows = await firstValueFrom(this.inventory.registrar(payload));
      this.saved.emit(rows);
    } catch (error) {
      this.error = (error as Error).message;
      this.openSections = Array.from(new Set([...this.openSections, 'origin']));
    } finally {
      this.busy = false;
      this.busyChange.emit(false);
    }
  }
}
