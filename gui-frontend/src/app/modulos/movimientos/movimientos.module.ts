import { NgModule } from '@angular/core';
import { SharedModule } from '../../compartido/shared.module';
import { MovimientosRoutingModule } from './movimientos-routing.module';
import { MovimientosComponent } from './movimientos.component';
@NgModule({
  declarations: [MovimientosComponent],
  imports: [SharedModule, MovimientosRoutingModule],
})
export class MovimientosModule {}
