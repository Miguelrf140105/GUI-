import { Component, OnInit } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../servicios/http/api.service';
@Component({
  selector: 'gui-password-recovery',
  templateUrl: './password-recovery.component.html',
  styleUrls: ['./password-recovery.component.scss'],
})
export class PasswordRecoveryComponent implements OnInit {
  email = '';
  code = '';
  password = '';
  repeat = '';
  requestId = '';
  busy = false;
  loading = true;
  available = false;
  done = false;
  error = '';
  notice = '';
  resendAt = 0;
  constructor(private api: ApiService) {}
  async ngOnInit() {
    try {
      const status = await firstValueFrom(
        this.api.get<{ available: boolean }>('/auth/recovery/status')
      );
      this.available = status.available;
    } catch (e) {
      this.error = (e as Error).message;
    } finally {
      this.loading = false;
    }
  }
  async request() {
    if (this.busy || !this.available) return;
    if (Date.now() < this.resendAt) {
      this.error = 'Espera un minuto antes de solicitar otro código.';
      return;
    }
    this.busy = true;
    this.error = '';
    try {
      const result = await firstValueFrom(
        this.api.post<{ requestId: string; message: string }>('/auth/recovery/request', {
          email: this.email.trim(),
        })
      );
      this.requestId = result.requestId;
      this.notice = result.message;
      this.code = '';
      this.resendAt = Date.now() + 60000;
    } catch (e) {
      this.error = (e as Error).message;
    } finally {
      this.busy = false;
    }
  }
  async complete() {
    if (this.busy || !this.requestId) return;
    if (this.password !== this.repeat) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }
    if (this.password.length < 14 || new TextEncoder().encode(this.password).length > 72) {
      this.error = 'Usa al menos 14 caracteres y evita una contraseña demasiado larga.';
      return;
    }
    this.busy = true;
    this.error = '';
    try {
      await firstValueFrom(
        this.api.post('/auth/recovery/complete', {
          requestId: this.requestId,
          code: this.code,
          password: this.password,
        })
      );
      this.done = true;
      this.code = '';
      this.password = '';
      this.repeat = '';
      this.requestId = '';
      this.notice = '';
    } catch (e) {
      this.error = (e as Error).message;
    } finally {
      this.busy = false;
    }
  }
}
