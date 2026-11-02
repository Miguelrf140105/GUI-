import { of, Subject } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { ProductPhotosComponent } from './product-photos.component';
import { ProductPhotosService } from '../../servicios/inventario/product-photos.service';
import { InicioSesionService } from '../../servicios/usuarios/login/iniciar-sesion.service';
describe('Fotos de producto', () => {
  function setup(admin = true) {
    const api = jasmine.createSpyObj('photos', ['list', 'upload', 'change', 'url']);
    api.list.and.returnValue(of([]));
    api.url.and.returnValue('');
    return { api, c: new ProductPhotosComponent(api, { can: () => admin } as any) };
  }
  it('un lector no puede subir, retirar ni cambiar portada', async () => {
    const { api, c } = setup(false);
    c.productId = 1;
    c.pendingRemove = 'photo';
    await c.change('photo', 'remove');
    await c.change('photo', 'cover');
    await c.upload({ target: { files: [new File(['x'], 'photo.png')], value: '' } } as any);
    expect(api.change).not.toHaveBeenCalled();
    expect(api.upload).not.toHaveBeenCalled();
  });
  it('no retira una foto sin la confirmación explícita', async () => {
    const { api, c } = setup();
    c.productId = 1;
    await c.change('photo', 'remove');
    expect(api.change).not.toHaveBeenCalled();
    c.pendingRemove = 'photo';
    api.change.and.returnValue(of(null));
    await c.change('photo', 'remove');
    expect(api.change).toHaveBeenCalledOnceWith(1, 'photo', 'remove');
  });
  it('descarta resultados antiguos al cambiar de producto', async () => {
    const { api, c } = setup();
    const old = new Subject<any>();
    api.list.and.returnValues(old, of([{ id: 'new' }]));
    c.productId = 1;
    const first = c.load();
    c.productId = 2;
    await c.load();
    old.next([{ id: 'old' }]);
    old.complete();
    await first;
    expect(c.photos[0].id).toBe('new');
  });
  it('bloquea doble envío y conserva el producto de la operación', async () => {
    const { api, c } = setup();
    c.productId = 1;
    const pending = new Subject<any>();
    api.change.and.returnValue(pending);
    const first = c.change('photo', 'cover');
    await c.change('photo', 'cover');
    expect(api.change).toHaveBeenCalledTimes(1);
    pending.next(null);
    pending.complete();
    await first;
    expect(c.busy).toBeFalse();
  });
  it('el formulario de subida no aparece a perfiles lectores', async () => {
    const { api } = setup();
    await TestBed.configureTestingModule({
      imports: [CommonModule],
      declarations: [ProductPhotosComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: ProductPhotosService, useValue: api },
        { provide: InicioSesionService, useValue: { can: () => false } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(ProductPhotosComponent);
    fixture.componentInstance.productId = 1;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('input[type=file]')).toBeNull();
  });
});
