import { NgModule } from '@angular/core';
import { SharedModule } from '../../compartido/shared.module';
import { RolesRoutingModule } from './roles-routing.module';
import { RolesComponent } from './roles.component';
@NgModule({ declarations: [RolesComponent], imports: [SharedModule, RolesRoutingModule] })
export class RolesModule {}
