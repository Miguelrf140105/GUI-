import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuditoriaComponent } from './auditoria.component';
const routes: Routes = [{ path: '', component: AuditoriaComponent }];
@NgModule({ imports: [RouterModule.forChild(routes)], exports: [RouterModule] })
export class AuditoriaRoutingModule {}
