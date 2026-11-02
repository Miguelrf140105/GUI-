import { NgModule } from '@angular/core';
import { SharedModule } from '../../compartido/shared.module';
import { UbicacionesRoutingModule } from './ubicaciones-routing.module';
import { UbicacionesComponent } from './ubicaciones.component';
@NgModule({
  declarations: [UbicacionesComponent],
  imports: [SharedModule, UbicacionesRoutingModule],
})
export class UbicacionesModule {}
