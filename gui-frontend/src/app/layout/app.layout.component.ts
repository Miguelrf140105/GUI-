import { Component, OnInit } from '@angular/core';
import { AppLayoutService } from './service/app.layout.service';
import { AplicacionService } from '../servicios/compartido/aplicacion.service';
@Component({
  selector: 'app-layout',
  templateUrl: './app.layout.component.html',
  styles: [':host { display: contents; }'],
})
export class AppLayoutComponent implements OnInit {
  constructor(public layout: AppLayoutService, public app: AplicacionService) {}
  ngOnInit(): void {
    this.app.cargar().catch((error) => (this.app.error = error.message));
  }
}
