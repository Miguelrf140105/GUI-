import { NgModule } from '@angular/core';
import { SharedModule } from '../../compartido/shared.module';
import { IngresosRoutingModule } from './ingresos-routing.module';
import { IngresosComponent } from './ingresos.component';
@NgModule({ declarations: [IngresosComponent], imports: [SharedModule, IngresosRoutingModule] })
export class IngresosModule {}
