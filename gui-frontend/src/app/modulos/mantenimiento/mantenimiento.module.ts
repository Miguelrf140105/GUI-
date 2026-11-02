import { NgModule } from '@angular/core';
import { SharedModule } from '../../compartido/shared.module';
import { MantenimientoRoutingModule } from './mantenimiento-routing.module';
import { MantenimientoComponent } from './mantenimiento.component';
@NgModule({
  declarations: [MantenimientoComponent],
  imports: [SharedModule, MantenimientoRoutingModule],
})
export class MantenimientoModule {}
