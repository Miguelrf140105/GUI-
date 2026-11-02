import { NgModule } from '@angular/core';
import { SharedModule } from '../../compartido/shared.module';
import { DocumentosRoutingModule } from './documentos-routing.module';
import { DocumentosComponent } from './documentos.component';
@NgModule({ declarations: [DocumentosComponent], imports: [SharedModule, DocumentosRoutingModule] })
export class DocumentosModule {}
