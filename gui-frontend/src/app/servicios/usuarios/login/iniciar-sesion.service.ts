import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, of, firstValueFrom } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ApiService } from '../../http/api.service';
import { Session } from '../../../models/shared.model';
@Injectable({ providedIn: 'root' })
export class InicioSesionService {
  readonly usuario$ = new BehaviorSubject<Session | null>(null);
  private checked = false;
  constructor(private api: ApiService) {}
  ensure(): Observable<Session | null> {
    if (this.checked) return of(this.usuario$.value);
    return this.api.get<Session>('/auth/me').pipe(
      tap((user) => {
        this.checked = true;
        this.usuario$.next(user);
      }),
      catchError(() => {
        this.checked = true;
        this.usuario$.next(null);
        return of(null);
      })
    );
  }
  async login(username: string, password: string): Promise<void> {
    this.api.resetCsrf();
    await firstValueFrom(
      this.api.post(
        '/auth/login',
        new HttpParams().set('username', username).set('password', password)
      )
    );
    this.api.resetCsrf();
    await firstValueFrom(this.api.token());
    await this.refresh();
  }
  async refresh(): Promise<void> {
    const user = await firstValueFrom(this.api.get<Session>('/auth/me'));
    this.checked = true;
    this.usuario$.next(user);
  }
  async logout(): Promise<void> {
    await firstValueFrom(this.api.post('/auth/logout', null));
    this.api.resetCsrf();
    this.usuario$.next(null);
    this.checked = true;
  }
  can(id: number): boolean {
    return !!this.usuario$.value?.permissions.includes(id);
  }
}
