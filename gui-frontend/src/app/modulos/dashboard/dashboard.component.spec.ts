import { of, throwError, Subject } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
describe('Dashboard: solicitudes frente a actividad del Kardex', () => {
  it('describe el contador de movimientos como solicitudes y conserva los eventos para la gráfica', () => {
    const c = new DashboardComponent({} as any, {} as any, {} as any, {} as any);
    c.stats = { assets: 25, movements: 0 };
    c.analytics = { monthly: [{ label: '2026-10', count: 25 }], categories: [] };
    const card = c.cards.find((item) => item.key === 'movements');
    expect(card?.detail).toBe('Solicitudes registradas');
    expect(c.stats.movements).toBe(0);
    expect(c.analytics.monthly[0].count).toBe(25);
    expect(c.points).not.toBe('');
  });
});

describe('Dashboard: recuperación y concurrencia', () => {
  function setup() {
    const api = jasmine.createSpyObj('api', ['pagina', 'dashboard']);
    api.dashboard.and.returnValue(of({ monthly: [], categories: [] }));
    return {
      api,
      c: new DashboardComponent({ can: () => false } as any, {} as any, api, {} as any),
    };
  }
  it('limpia el error al recuperar la conexión', async () => {
    const { c, api } = setup();
    api.pagina.and.returnValue(throwError(() => new Error('Sin conexión')));
    await c.load();
    expect(c.error).toBe('Sin conexión');
    api.pagina.and.returnValue(of({ rows: [{ assets: 25 }] }));
    await c.load();
    expect(c.error).toBe('');
    expect(c.stats.assets).toBe(25);
  });
  it('descarta respuestas anteriores que llegan tarde', async () => {
    const { c, api } = setup();
    const old = new Subject<any>();
    api.pagina.and.returnValues(old, of({ rows: [{ assets: 25 }] }));
    const first = c.load();
    await c.load();
    old.next({ rows: [{ assets: 1 }] });
    old.complete();
    await first;
    expect(c.stats.assets).toBe(25);
  });
  it('da un fondo CSS válido sin activos', () => {
    expect(setup().c.gradient).toBe('#dbe8df');
  });
});
