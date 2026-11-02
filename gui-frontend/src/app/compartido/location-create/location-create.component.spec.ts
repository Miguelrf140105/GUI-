import { of } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { LocationCreateComponent } from './location-create.component';
import { AplicacionService } from '../../servicios/compartido/aplicacion.service';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { SharedModule } from '../shared.module';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('Alta guiada de ubicaciones', () => {
  const types = [
    { id: 201, groupId: 2, name: 'Sede' },
    { id: 202, groupId: 2, name: 'Pabellón' },
    { id: 203, groupId: 2, name: 'Piso' },
    { id: 204, groupId: 2, name: 'Laboratorio' },
    { id: 206, groupId: 2, name: 'Aula' },
  ];
  const locations = [
    { id: 1, name: 'UNID', typeId: 201, parentId: null, responsibleId: 7 },
    ...Array.from({ length: 8 }, (_, i) => ({
      id: 10 + i,
      name: 'Piso ' + (i + 1),
      typeId: 203,
      parentId: 1,
    })),
    { id: 50, name: 'Laboratorio', typeId: 204, parentId: 10, responsibleId: 7 },
  ];
  const app = {
    options: (key: string) =>
      key === 'parameters' ? types : key === 'people' ? [{ id: 7, name: 'Responsable UNID' }] : [],
  };
  function setup(context: number | null = null) {
    const api = jasmine.createSpyObj('catalogos', ['guardar']);
    api.guardar.and.returnValue(of({ id: 300 }));
    const c = new LocationCreateComponent(app as any, api);
    c.locations = locations;
    c.contextParentId = context;
    c.ngOnInit();
    return { c, api };
  }
  it('preselecciona la única sede pero no adivina un piso para crear un ambiente', () => {
    const { c } = setup();
    expect(c.siteId).toBe(1);
    expect(c.floorId).toBeNull();
    expect(c.parentId).toBeNull();
    expect(c.spaceTypes.map((row) => row.id)).toEqual([204, 206]);
  });
  it('aprovecha el piso seleccionado y propone el siguiente número para un piso nuevo', () => {
    const { c } = setup(12);
    expect(c.level).toBe('room');
    expect(c.parentId).toBe(12);
    expect(c.previewPath).toContain('Piso 3');
    c.setLevel('floor');
    expect(c.floorNumber).toBe(9);
    expect(c.finalName).toBe('Piso 9');
    expect(c.finalType).toBe(203);
    expect(c.parentId).toBe(1);
  });
  it('crea un subambiente dentro del ambiente elegido sin usarlo como espacio nuevo', () => {
    const { c } = setup(50);
    expect(c.level).toBe('subroom');
    expect(c.roomId).toBe(50);
    expect(c.parentId).toBe(50);
    expect(c.inheritedResponsible).toBe('Responsable UNID');
  });
  it('envía solo datos necesarios y deja código y responsable heredado al backend', async () => {
    const { c, api } = setup(10);
    c.name = 'Aula nueva';
    c.typeId = 206;
    await c.submit({ invalid: false } as any);
    expect(api.guardar).toHaveBeenCalledOnceWith('/locations', {
      name: 'Aula nueva',
      typeId: 206,
      parentId: 10,
      responsibleId: null,
    });
    expect(api.guardar.calls.mostRecent().args[1].code).toBeUndefined();
  });
  it('no guarda un ambiente sin padre válido ni borra datos cuando falla el guardado', async () => {
    const { c, api } = setup();
    c.name = 'Aula';
    c.typeId = 206;
    const touch = jasmine.createSpy();
    await c.submit({ invalid: false, control: { markAllAsTouched: touch } } as any);
    expect(api.guardar).not.toHaveBeenCalled();
    expect(touch).toHaveBeenCalled();
  });
  it('la plantilla de alta de ambiente no solicita código ni un ambiente existente', async () => {
    await TestBed.configureTestingModule({
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        { provide: AplicacionService, useValue: app },
        { provide: CatalogosService, useValue: { guardar: () => of({}) } },
      ],
    }).compileComponents();
    const f = TestBed.createComponent(LocationCreateComponent);
    f.componentInstance.locations = locations;
    f.detectChanges();
    await f.whenStable();
    expect(f.nativeElement.querySelector('input[name=code]')).toBeNull();
    expect(f.nativeElement.querySelector('[name=roomId]')).toBeNull();
    expect(f.nativeElement.textContent).toContain('Código único generado al guardar');
    f.destroy();
  });

  for (const test of [
    { level: 'site', name: 'Sede nueva', type: 201, parent: null, floor: null, room: null },
    { level: 'floor', name: 'Piso 9', type: 203, parent: 1, floor: null, room: null },
    { level: 'room', name: 'Aula nueva', type: 206, parent: 10, floor: 10, room: null },
    { level: 'subroom', name: 'Área de preparación', type: 204, parent: 50, floor: 10, room: 50 },
  ]) {
    it('envía el nombre, tipo y padre correctos para ' + test.level, async () => {
      const { c, api } = setup();
      c.setLevel(test.level as any);
      c.siteId = 1;
      c.floorId = test.floor;
      c.roomId = test.room;
      c.name = test.name;
      c.typeId = test.type;
      c.floorNumber = 9;
      await c.submit({ invalid: false } as any);
      expect(api.guardar).toHaveBeenCalledOnceWith('/locations', {
        name: test.name,
        typeId: test.type,
        parentId: test.parent,
        responsibleId: null,
      });
    });
  }
});
