import { Injectable } from '@angular/core';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { CatalogosService } from './catalogos.service';
import { Lookups } from '../../models/shared.model';
@Injectable({ providedIn: 'root' })
export class AplicacionService {
  lookups: Lookups = {};
  readonly refresh$ = new BehaviorSubject(0);
  notice = '';
  error = '';
  constructor(private catalogos: CatalogosService) {}
  async cargar(): Promise<void> {
    this.lookups = await firstValueFrom(this.catalogos.lookups());
  }
  options(key: string): any[] {
    return this.lookups[key] || [];
  }
  async refresh(message = 'Registro guardado correctamente.'): Promise<void> {
    await this.cargar();
    this.refresh$.next(this.refresh$.value + 1);
    this.notice = message;
    setTimeout(() => (this.notice = ''), 5000);
  }
}
