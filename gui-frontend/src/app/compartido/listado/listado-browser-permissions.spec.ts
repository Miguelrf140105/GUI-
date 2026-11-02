import { ProductPhotosService } from '../../servicios/inventario/product-photos.service';
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { NEVER, of } from 'rxjs';
import { SharedModule } from '../shared.module';
import { ListadoComponent } from './listado.component';
import { AplicacionService } from '../../servicios/compartido/aplicacion.service';
import { InicioSesionService } from '../../servicios/usuarios/login/iniciar-sesion.service';
import { CatalogosService } from '../../servicios/compartido/catalogos.service';
import { InventarioService } from '../../servicios/inventario/inventario.service';

describe('HTML real de permisos en navegador, sin base de datos', () => {
 let admin = false;
 const locations = [
  {id: 1, name: 'Sede QA', parentId: null, stateId: 1},
  {id: 2, name: 'Piso QA', parentId: 1, stateId: 1},
  {id: 3, name: 'Ambiente QA', parentId: 2, stateId: 1},
  {id: 4, name: 'Subambiente QA', parentId: 3, stateId: 1},
 ];
 const api = jasmine.createSpyObj('catalogos', ['guardar','pagina','arbolUbicaciones']);
 beforeEach(async () => {
  admin=false;api.guardar.calls.reset();
  api.pagina.and.returnValue(of({rows: [], page: 0, size: 50, hasMore: false}));
  api.arbolUbicaciones.and.returnValue(of(locations));
  await TestBed.configureTestingModule({
   imports:[SharedModule,RouterTestingModule,NoopAnimationsModule],
   providers:[
    {provide:AplicacionService,useValue:{refresh$:NEVER,options:()=>[],lookups:{}}},
    // Otro perfil puede tener gestión de creación, pero no edición exclusiva ni baja.
    {provide:InicioSesionService,useValue:{can:(id:number)=>admin || ![303,304,503,504,902,1002,1303,1403,1502].includes(id)}},
    {provide:CatalogosService,useValue:api},
    {provide:InventarioService,useValue:{}},
    {provide:ProductPhotosService,useValue:{url:()=>'',list:()=>of([])}},
   ]
  }).compileComponents();
 });
 async function render(moduleId:number) {
  const fixture=TestBed.createComponent(ListadoComponent);
  fixture.componentInstance.moduleId=moduleId;
  fixture.componentInstance.locationTree=locations;
  fixture.componentInstance.result={rows:[{id:2,name:'Registro QA',code:'QA-0001',stateId:1}],page:0,size:50,hasMore:false};
  fixture.detectChanges();await fixture.whenStable();fixture.detectChanges();return fixture;
 }
 for(const profile of ['Avanzado','Limitado']) {
  for(const moduleId of [30,50,90,100,130,140]) {
   it(profile+' no muestra editar ni eliminar en módulo '+moduleId, async () => {
    const fixture=await render(moduleId);
    const element:HTMLElement=fixture.nativeElement;
    expect(element.querySelectorAll('button[aria-label^="Editar "],button[aria-label^="Eliminar "]').length).toBe(0);
    if(moduleId===50) expect(element.querySelectorAll('button[aria-label^="Añadir espacio"]').length).toBeGreaterThan(0);
    expect(api.guardar).not.toHaveBeenCalled();
   });
  }
 }
 it('Administrador dispone de edición y baja en los cuatro niveles', async () => {
  admin=true;const fixture=await render(50);const element:HTMLElement=fixture.nativeElement;
  for(const name of ['Piso QA','Ambiente QA']) {
   (element.querySelector('button[aria-label="Expandir '+name+'"]') as HTMLButtonElement).click();fixture.detectChanges();
  }
  for(const row of locations) {
   expect(element.querySelector('button[aria-label="Editar '+row.name+'"]')).not.toBeNull();
   expect(element.querySelector('button[aria-label="Eliminar '+row.name+'"]')).not.toBeNull();
  }
  (element.querySelector('button[aria-label="Eliminar Sede QA"]') as HTMLButtonElement).click();fixture.detectChanges();
  expect(element.textContent).toContain('Esta ubicación contiene otros espacios');
  const confirm=Array.from(element.querySelectorAll('button')).find(b=>b.textContent?.includes('Eliminar ubicación'));
  expect(confirm?.disabled).toBeTrue();expect(api.guardar).not.toHaveBeenCalled();
 });
});
