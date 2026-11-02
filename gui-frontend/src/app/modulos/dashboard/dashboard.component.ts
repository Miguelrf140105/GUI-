import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, firstValueFrom } from 'rxjs';
import { AplicacionService } from '../../servicios/compartido/aplicacion.service';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { InicioSesionService } from '../../servicios/usuarios/login/iniciar-sesion.service';
import { Row } from '../../models/shared.model';
import { paths } from '../../layout/api/navigation';
@Component({
  selector: 'gui-dashboard',
  templateUrl: './dashboard.component.html',
  styles: [':host { display: contents; }'],
})
export class DashboardComponent implements OnInit, OnDestroy {
  stats: Row = {};
  recent: Row[] = [];
  analytics: { monthly: Row[]; categories: Row[] } = { monthly: [], categories: [] };
  loading = false;
  error = '';
  modal = false;
  busy = false;
  printAssets: Row[] = [];
  today = new Date();
  private subscription?: Subscription;
  private generation = 0;
  cards = [
    {
      key: 'assets',
      label: 'Activos registrados',
      icon: 'Package',
      color: 'teal',
      detail: 'Bienes identificados',
      module: 30,
    },
    {
      key: 'movements',
      label: 'Movimientos del mes',
      icon: 'ArrowLeftRight',
      color: 'blue',
      detail: 'Solicitudes registradas',
      module: 40,
    },
    {
      key: 'maintenance',
      label: 'En mantenimiento',
      icon: 'Wrench',
      color: 'amber',
      detail: 'Activos en revisión',
      module: 60,
    },
    {
      key: 'pending',
      label: 'Por autorizar',
      icon: 'Clock3',
      color: 'purple',
      detail: 'Solicitudes pendientes',
      module: 40,
    },
  ];
  constructor(
    public auth: InicioSesionService,
    public app: AplicacionService,
    private api: CatalogosService,
    private router: Router
  ) {}
  ngOnInit(): void {
    this.subscription = this.app.refresh$.subscribe(() => this.load());
  }
  ngOnDestroy(): void {
    this.generation++;
    this.subscription?.unsubscribe();
  }
  async load(): Promise<void> {
    // Solo la carga más reciente publica resultados, incluso al salir de la pantalla.
    const version = ++this.generation;
    this.loading = true;
    this.error = '';
    try {
      const [page, analytics, recent] = await Promise.all([
        firstValueFrom(this.api.pagina(10)),
        firstValueFrom(this.api.dashboard()),
        this.auth.can(401) ? firstValueFrom(this.api.pagina(40)) : Promise.resolve({ rows: [] }),
      ]);
      if (version === this.generation) {
        this.stats = page.rows[0] || {};
        this.analytics = analytics;
        this.recent = recent.rows.slice(0, 5);
      }
    } catch (error) {
      if (version === this.generation) this.error = (error as Error).message;
    } finally {
      if (version === this.generation) this.loading = false;
    }
  }
  go(id: number): void {
    this.router.navigate(['/' + paths[id]]);
  }
  get total(): number {
    return this.analytics.categories.reduce((sum, item) => sum + Number(item.count), 0);
  }
  get points(): string {
    const max = Math.max(1, ...this.analytics.monthly.map((item) => Number(item.count)));
    return this.analytics.monthly
      .map(
        (item, index) =>
          45 +
          (index * 590) / Math.max(this.analytics.monthly.length - 1, 1) +
          ',' +
          (180 - (Number(item.count) / max) * 130)
      )
      .join(' ');
  }
  get gradient(): string {
    // Evita un conic-gradient vacío cuando no hay activos.
    if (!this.total) return '#dbe8df';
    let sum = 0;
    const colors = ['#13877d', '#64ada1', '#aacdc4', '#dbe8df'];
    return (
      'conic-gradient(' +
      this.analytics.categories
        .map((item, index) => {
          const from = sum;
          sum += this.total ? (Number(item.count) / this.total) * 100 : 0;
          return colors[index % 4] + ' ' + from + '% ' + sum + '%';
        })
        .join(',') +
      ')'
    );
  }
  async saved(rows: Row[] = []): Promise<void> {
    this.modal = false;
    this.printAssets = rows;
    try {
      await this.app.refresh();
    } catch (error) {
      this.error = (error as Error).message;
    }
  }
}
