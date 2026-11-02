import { TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { ApiService } from '../servicios/http/api.service';
import { of, Subject } from 'rxjs';
import { PasswordRecoveryComponent } from './password-recovery.component';
import { ListadoComponent } from '../compartido/listado/listado.component';
describe('Recuperación de contraseña', () => {
  function setup() {
    const api = jasmine.createSpyObj('api', ['get', 'post']);
    return { api, c: new PasswordRecoveryComponent(api) };
  }
  it('no solicita códigos si el correo no está disponible', async () => {
    const { api, c } = setup();
    api.get.and.returnValue(of({ available: false }));
    await c.ngOnInit();
    await c.request();
    expect(c.available).toBeFalse();
    expect(api.post).not.toHaveBeenCalled();
  });
  it('conserva el identificador y evita reenvíos inmediatos', async () => {
    const { api, c } = setup();
    c.available = true;
    c.email = ' persona@example.invalid ';
    api.post.and.returnValue(
      of({
        requestId: 'id',
        message: 'Si el correo está registrado, recibirás un código de recuperación.',
      })
    );
    await c.request();
    await c.request();
    expect(api.post).toHaveBeenCalledTimes(1);
    expect(c.requestId).toBe('id');
    expect(api.post.calls.first().args[1]).toEqual({ email: 'persona@example.invalid' });
  });
  it('no envía contraseñas que no coinciden o son débiles', async () => {
    const { api, c } = setup();
    c.requestId = 'id';
    c.password = 'Primera-Password-123';
    c.repeat = 'Otra-Password-123';
    await c.complete();
    expect(api.post).not.toHaveBeenCalled();
    c.password = c.repeat = 'short';
    await c.complete();
    expect(api.post).not.toHaveBeenCalled();
  });
  it('un envío por confirmación y limpia los secretos al terminar', async () => {
    const { api, c } = setup();
    c.requestId = 'id';
    c.code = '00123456';
    c.password = c.repeat = 'Nueva-Password-123';
    const pending = new Subject<any>();
    api.post.and.returnValue(pending);
    const first = c.complete();
    await c.complete();
    expect(api.post).toHaveBeenCalledTimes(1);
    expect(api.post.calls.first().args[1].code).toBe('00123456');
    pending.next(null);
    pending.complete();
    await first;
    expect(c.done).toBeTrue();
    expect(c.password).toBe('');
    expect(c.code).toBe('');
  });
});
describe('Papelera de inventario', () => {
  it('no permite abrir ni ejecutar el borrado a un lector', async () => {
    const api = jasmine.createSpyObj('api', ['guardar']);
    const c = new ListadoComponent({} as any, { can: () => false } as any, api, {} as any);
    c.moduleId = 30;
    c.requestAssetDelete({ id: 1 });
    expect(c.assetDelete).toBeNull();
    c.assetDelete = { id: 1 };
    await c.deleteAsset();
    expect(api.guardar).not.toHaveBeenCalled();
  });
  it('doble clic genera una sola anulación y mantiene el error en pantalla', async () => {
    const api = jasmine.createSpyObj('api', ['guardar']);
    const pending = new Subject<any>();
    api.guardar.and.returnValue(pending);
    const c = new ListadoComponent(
      { refresh: () => Promise.resolve() } as any,
      { can: () => true } as any,
      api,
      {} as any
    );
    c.moduleId = 30;
    c.assetDelete = { id: 1 };
    const first = c.deleteAsset();
    await c.deleteAsset();
    expect(api.guardar).toHaveBeenCalledTimes(1);
    pending.error(new Error('El producto tuvo movimientos; no se puede eliminar.'));
    await first;
    expect(c.assetDelete).not.toBeNull();
    expect(c.assetDeleteError).toContain('movimientos');
  });
});

describe('Recuperación: formulario renderizado', () => {
  async function render(available: boolean) {
    const api = jasmine.createSpyObj('api', ['get', 'post']);
    api.get.and.returnValue(of({ available }));
    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule, RouterTestingModule],
      declarations: [PasswordRecoveryComponent],
      providers: [{ provide: ApiService, useValue: api }],
    }).compileComponents();
    const fixture = TestBed.createComponent(PasswordRecoveryComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return { api, fixture, el: fixture.nativeElement as HTMLElement };
  }
  it('mantiene el envío deshabilitado hasta introducir un correo válido', async () => {
    const { fixture, el } = await render(true);
    const button = el.querySelector('button[type=submit]') as HTMLButtonElement;
    expect(button.disabled).toBeTrue();
    const input = el.querySelector('input[type=email]') as HTMLInputElement;
    input.value = 'qa@example.invalid';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(button.disabled).toBeFalse();
  });
  it('no presenta formularios ni promete envíos cuando falta el remitente', async () => {
    const { el } = await render(false);
    expect(el.querySelector('form')).toBeNull();
    expect(el.textContent).toContain('no está disponible');
    expect(el.querySelector('a')?.getAttribute('href')).toBe('/login');
  });
});
