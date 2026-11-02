import { NgModule } from '@angular/core';
import { SharedModule } from '../../compartido/shared.module';
import { AuditoriaRoutingModule } from './auditoria-routing.module';
import { AuditoriaComponent } from './auditoria.component';
@NgModule({ declarations: [AuditoriaComponent], imports: [SharedModule, AuditoriaRoutingModule] })
export class AuditoriaModule {}
