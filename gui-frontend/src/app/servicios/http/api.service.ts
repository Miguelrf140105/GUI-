import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

/** Transporte único: cookies de sesión y CSRF; el navegador nunca conoce SQL Server. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private csrf?: { token: string; headerName: string };
  constructor(private http: HttpClient) {}
  resetCsrf(): void {
    this.csrf = undefined;
  }
  token(): Observable<{ token: string; headerName: string }> {
    return this.csrf
      ? of(this.csrf)
      : this.http
          .get<{ token: string; headerName: string }>(environment.apiRest.host + '/auth/csrf', {
            withCredentials: true,
          })
          .pipe(tap((token) => (this.csrf = token)));
  }
  get<T = any>(path: string): Observable<T> {
    return this.http
      .get<T>(environment.apiRest.host + path, { withCredentials: true })
      .pipe(catchError((error) => this.failure(error)));
  }
  post<T = any>(path: string, body: unknown): Observable<T> {
    return this.token().pipe(
      switchMap((token) =>
        this.http.post<T>(environment.apiRest.host + path, body, {
          withCredentials: true,
          headers: new HttpHeaders({ [token.headerName]: token.token }),
        })
      ),
      catchError((error) => this.failure(error))
    );
  }
  private failure(error: any): Observable<never> {
    if (error.status === 401 || error.status === 403) this.resetCsrf();
    const message =
      error.status === 401
        ? 'Tu sesión terminó o las credenciales son incorrectas.'
        : error.status === 403
        ? 'No tienes permiso para esta acción.'
        : error.status === 429
        ? 'Demasiados intentos. Espera unos minutos.'
        : error.error?.detail ||
          error.error?.message ||
          (error.status === 0
            ? 'No se pudo conectar. Inicia GUI Local desde IntelliJ.'
            : 'No se pudo completar la solicitud.');
    return throwError(() => new Error(message));
  }
}
