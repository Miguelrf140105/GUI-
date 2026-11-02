import { locationPath, validParents, csvText } from './ubicaciones.util';
describe('jerarquías mutables', () => {
  const locations = [
    { id: 1, name: 'Sede', parentId: null },
    { id: 2, name: 'Pabellón', parentId: 1 },
    { id: 3, name: 'Laboratorio', parentId: 2 },
    { id: 4, name: 'Almacén', parentId: 1 },
  ];
  it('construye rutas sin codificar niveles fijos', () =>
    expect(locationPath(3, locations)).toBe('Sede / Pabellón / Laboratorio'));
  it('impide seleccionar el nodo o sus descendientes como padre', () =>
    expect(validParents(2, locations).map((l) => l.id)).toEqual([1, 4]));
  it('termina aun si recibe datos con un ciclo', () =>
    expect(
      locationPath(1, [
        { id: 1, name: 'A', parentId: 2 },
        { id: 2, name: 'B', parentId: 1 },
      ])
    ).toBe('B / A'));
});
it('exportación neutraliza fórmulas y escapa comillas', () =>
  expect(csvText([{ name: '=HYPERLINK("bad")' }], [{ key: 'name', label: 'Nombre' }])).toContain(
    '"\'=HYPERLINK(""bad"")"'
  ));
