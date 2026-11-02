import { LocationPickerComponent } from './location-picker.component';
describe('Selección jerárquica de ubicaciones', () => {
  it('aplica el filtro del árbol al elegir un piso, sin exigir un ambiente', () => {
    const p = picker();
    p.autoSelectBranch = true;
    p.choose(0, 1);
    p.choose(1, 4);
    expect(p.value).toBe(4);
    expect(p.selectedPath).toBe('UNID / Piso 2');
  });
  function picker() {
    const component = new LocationPickerComponent();
    component.locations = [
      { id: 1, name: 'UNID' },
      { id: 2, parentId: 1, name: 'Almacén central' },
      { id: 3, parentId: 1, name: 'Piso 1' },
      { id: 4, parentId: 1, name: 'Piso 2' },
      { id: 5, parentId: 3, name: 'Aula 101' },
      { id: 6, parentId: 4, name: 'Aula 201' },
      { id: 7, parentId: 3, name: 'Administración' },
      { id: 8, parentId: 7, name: 'Oficina' },
      { id: 9, name: 'Otra sede' },
      { id: 10, parentId: 9, name: 'Depósito' },
    ];
    component.ngOnChanges();
    return component;
  }
  it('filtra por sede y piso, emite el ID real y descarta el ambiente al cambiar piso', () => {
    const p = picker();
    const changed = jasmine.createSpy();
    p.registerOnChange(changed);
    p.choose(0, 1);
    expect(p.steps[1].options.map((r) => r.id)).toEqual([3, 4, 0]);
    p.choose(1, 3);
    expect(p.steps[2].options.map((r) => r.id)).toEqual([5, 7]);
    p.choose(2, 5);
    expect(changed).toHaveBeenCalledWith(5);
    p.choose(1, 4);
    expect(p.value).toBeNull();
    expect(p.steps[2].value).toBeNull();
    expect(p.steps[2].options.map((r) => r.id)).toEqual([6]);
    p.choose(0, 9);
    expect(p.path).toEqual([9]);
    expect(p.value).toBeNull();
  });
  it('restaura un subambiente existente y permite seleccionar explícitamente un piso', () => {
    const p = picker();
    p.writeValue(8);
    expect(p.path).toEqual([1, 3, 7, 8]);
    expect(p.steps[3].label).toBe('Subambiente');
    expect(p.selectedPath).toBe('UNID / Piso 1 / Administración / Oficina');
    p.choose(1, 4);
    p.useBranch();
    expect(p.value).toBe(4);
  });
  it('incluye almacenes sin piso sin inventarles un piso y valida datos desconocidos', () => {
    const p = picker();
    p.writeValue(2);
    expect(p.path).toEqual([1, 0, 2]);
    expect(p.steps[2].options.map((r) => r.id)).toEqual([2]);
    p.writeValue(999);
    expect(p.validate()).toEqual({ location: true });
    p.required = true;
    p.clear();
    expect(p.validate()).toEqual({ required: true });
  });
  it('respeta ramas de alcance parcial y no cambia cuando está deshabilitado', () => {
    const p = picker();
    p.locations = p.locations.filter((row) => [7, 8].includes(row.id));
    p.ngOnChanges();
    expect(p.steps[0].options.map((row) => row.id)).toEqual([7]);
    p.setDisabledState(true);
    p.choose(0, 7);
    expect(p.path).toEqual([]);
  });
});
