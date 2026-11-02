import { NgModule } from '@angular/core';
import { SharedModule } from '../../compartido/shared.module';
import { ProveedoresRoutingModule } from './proveedores-routing.module';
import { ProveedoresComponent } from './proveedores.component';
@NgModule({
  declarations: [ProveedoresComponent],
  imports: [SharedModule, ProveedoresRoutingModule],
})
export class ProveedoresModule {}
