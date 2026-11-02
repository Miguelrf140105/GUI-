import { LocationTreeComponent } from './location-tree.component';

describe('Ramas de ubicaciones', () => {
  it('permite ver una rama autorizada cuyo padre queda fuera del alcance', () => {
    const tree = new LocationTreeComponent();
    tree.locations = [
      { id: 20, parentId: 10 },
      { id: 21, parentId: 20 },
    ];
    expect(tree.children(null, []).map((row) => row.id)).toEqual([20]);
    expect(tree.children(20, [20]).map((row) => row.id)).toEqual([21]);
  });
  it('mantiene el piso cerrado hasta desplegarlo y corta ciclos', () => {
    const tree = new LocationTreeComponent();
    const floor = { id: 2, parentId: 1 };
    tree.locations = [{ id: 1, parentId: null }, floor];
    expect(tree.isExpanded(floor)).toBeFalse();
    tree.toggle(floor);
    expect(tree.isExpanded(floor)).toBeTrue();
    expect(tree.children(1, [2])).toEqual([]);
  });
});
