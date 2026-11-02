import { ProductPhotosComponent } from './product-photos/product-photos.component';
import { ProductThumbnailComponent } from './product-photos/product-thumbnail.component';
import { EgresoDetalleComponent } from './egreso-detalle/egreso-detalle.component';
import { IntakeDeleteComponent } from './intake-delete/intake-delete.component';
import { LocationCreateComponent } from './location-create/location-create.component';
import { AssetNameComponent } from './asset-name/asset-name.component';
import { QrSelectorComponent } from './qr-selector/qr-selector.component';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { DropdownModule } from 'primeng/dropdown';
import { LoadingModule } from './loading.module';
import { IconComponent } from './icon/icon.component';
import { BadgeComponent } from './badge/badge.component';
import { EmptyComponent } from './empty/empty.component';
import { SearchSelectComponent } from './search-select/search-select.component';
import { AccordionComponent } from './accordion/accordion.component';
import { ModalComponent } from './modal/modal.component';
import { EditorComponent } from './editor/editor.component';
import { InventarioRegistroComponent } from './inventario-registro/inventario-registro.component';
import { ArchivosComponent } from './archivos/archivos.component';
import { QrComponent } from './qr/qr.component';
import { FichaComponent } from './ficha/ficha.component';
import { IngresoDetalleComponent } from './ingreso-detalle/ingreso-detalle.component';
import { LocationTreeComponent } from './location-tree/location-tree.component';
import { LocationPickerComponent } from './location-picker/location-picker.component';
import { QrScannerComponent } from './qr-scanner/qr-scanner.component';
import { ListadoComponent } from './listado/listado.component';
import { QrRouteComponent } from './qr-route/qr-route.component';
@NgModule({
  declarations: [
    ProductPhotosComponent,
    ProductThumbnailComponent,
    EgresoDetalleComponent,
    IntakeDeleteComponent,
    LocationCreateComponent,
    AssetNameComponent,
    QrSelectorComponent,
    LocationPickerComponent,
    QrScannerComponent,
    IconComponent,
    BadgeComponent,
    EmptyComponent,
    SearchSelectComponent,
    AccordionComponent,
    ModalComponent,
    EditorComponent,
    InventarioRegistroComponent,
    ArchivosComponent,
    QrComponent,
    FichaComponent,
    IngresoDetalleComponent,
    LocationTreeComponent,
    ListadoComponent,
    QrRouteComponent,
  ],
  imports: [CommonModule, FormsModule, RouterModule, DropdownModule, LoadingModule],
  exports: [
    ProductPhotosComponent,
    ProductThumbnailComponent,
    EgresoDetalleComponent,
    IntakeDeleteComponent,
    LocationCreateComponent,
    AssetNameComponent,
    QrSelectorComponent,
    LocationPickerComponent,
    QrScannerComponent,
    CommonModule,
    FormsModule,
    RouterModule,
    DropdownModule,
    LoadingModule,
    IconComponent,
    BadgeComponent,
    EmptyComponent,
    SearchSelectComponent,
    AccordionComponent,
    ModalComponent,
    EditorComponent,
    InventarioRegistroComponent,
    ArchivosComponent,
    QrComponent,
    FichaComponent,
    IngresoDetalleComponent,
    LocationTreeComponent,
    ListadoComponent,
    QrRouteComponent,
  ],
})
export class SharedModule {}
