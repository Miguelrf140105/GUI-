import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { InicioSesionService } from '../servicios/usuarios/login/iniciar-sesion.service';
import { AppLayoutService } from './service/app.layout.service';
import { AplicacionService } from '../servicios/compartido/aplicacion.service';
@Component({
  selector: 'app-topbar',
  templateUrl: './app.topbar.component.html',
  styles: [':host { display: contents; }'],
})
export class AppTopbarComponent {
  institution = environment.institution;
  constructor(
    public auth: InicioSesionService,
    public layout: AppLayoutService,
    private router: Router,
    public app: AplicacionService
  ) {}
  get initials(): string {
    return (this.auth.usuario$.value?.name || '')
      .split(' ')
      .slice(0, 2)
      .map((value) => value[0])
      .join('');
  }
  async logout(): Promise<void> {
    try {
      await this.auth.logout();
      await this.router.navigate(['/login']);
    } catch (error) {
      this.app.error = (error as Error).message;
    }
  }
}
