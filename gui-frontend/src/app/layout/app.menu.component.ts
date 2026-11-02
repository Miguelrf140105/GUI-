import { Component } from '@angular/core';
import { modules } from '../models/modulos.model';
import { paths } from './api/navigation';
import { InicioSesionService } from '../servicios/usuarios/login/iniciar-sesion.service';
import { AppLayoutService } from './service/app.layout.service';
@Component({
  selector: 'app-menu',
  templateUrl: './app.menu.component.html',
  styles: [':host { display: contents; }'],
})
export class AppMenuComponent {
  items = modules;
  paths = paths;
  constructor(public auth: InicioSesionService, public layout: AppLayoutService) {}
}
