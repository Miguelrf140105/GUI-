import { of } from 'rxjs';
import { IngresoDetalleComponent } from './ingreso-detalle.component';
describe('Detalle del ingreso y códigos estables',()=>{
 it('muestra ING0001 conservando el ID numérico para consultar',async()=>{
  const api=jasmine.createSpyObj('catalogos',['ingreso']);api.ingreso.and.returnValue(of({lines:[],documents:[],units:[{id:24,code:'MIC-000024'}]}));
  const c=new IngresoDetalleComponent(api);c.row={id:1,code:'ING0001'};await c.ngOnInit();
  expect(c.code).toBe('ING0001');expect(api.ingreso).toHaveBeenCalledOnceWith(1);expect(c.detail?.units?.[0].code).toBe('MIC-000024');
 });
 it('mantiene compatibilidad de presentación y no trunca correlativos largos',()=>{
  const c=new IngresoDetalleComponent({} as any);c.row={id:2};expect(c.code).toBe('ING0002');c.row={id:10000};expect(c.code).toBe('ING10000');
 });
});
