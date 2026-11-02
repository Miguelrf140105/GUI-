import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { IngresosComponent } from './ingresos.component';
const routes: Routes = [{ path: '', component: IngresosComponent }];
@NgModule({ imports: [RouterModule.forChild(routes)], exports: [RouterModule] })
export class IngresosRoutingModule {}
