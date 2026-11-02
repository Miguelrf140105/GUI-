import { NgModule, Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../compartido/shared.module';
@Component({
  selector: 'app-egresos',
  template: '<gui-listado [moduleId]="150"></gui-listado>',
  styles: [':host {display:contents}'],
})
export class EgresosComponent {}
@NgModule({
  declarations: [EgresosComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: EgresosComponent }])],
})
export class EgresosModule {}
