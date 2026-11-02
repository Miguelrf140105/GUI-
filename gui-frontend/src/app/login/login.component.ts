import { modules } from '../models/modulos.model';
import { paths } from '../layout/api/navigation';
import { Component } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { InicioSesionService } from '../servicios/usuarios/login/iniciar-sesion.service';
import { environment } from '../../environments/environment';
@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styles: [':host { display: contents; }'],
})
export class LoginComponent {
  username = '';
  password = '';
  busy = false;
  error = '';
  institution = environment.institution;
  constructor(
    private auth: InicioSesionService,
    private router: Router,
    private route: ActivatedRoute
  ) {}
  async submit(): Promise<void> {
    if (this.busy) return;
    this.busy = true;
    this.error = '';
    try {
      await this.auth.login(this.username, this.password);
      const target = this.route.snapshot.queryParamMap.get('returnUrl');
      await this.router.navigateByUrl(
        target?.startsWith('/') && !target.startsWith('//')
          ? target
          : '/' + paths[modules.find((item) => this.auth.can(item.id * 10 + 1))?.id || 10]
      );
    } catch (error) {
      this.error = (error as Error).message;
    } finally {
      this.busy = false;
    }
  }
}
