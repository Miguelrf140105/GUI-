import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UbicacionesComponent } from './ubicaciones.component';
const routes: Routes = [{ path: '', component: UbicacionesComponent }];
@NgModule({ imports: [RouterModule.forChild(routes)], exports: [RouterModule] })
export class UbicacionesRoutingModule {}
