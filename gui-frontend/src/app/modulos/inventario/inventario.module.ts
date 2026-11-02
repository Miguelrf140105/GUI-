import { NgModule } from '@angular/core';
import { SharedModule } from '../../compartido/shared.module';
import { InventarioRoutingModule } from './inventario-routing.module';
import { InventarioComponent } from './inventario.component';
@NgModule({ declarations: [InventarioComponent], imports: [SharedModule, InventarioRoutingModule] })
export class InventarioModule {}
