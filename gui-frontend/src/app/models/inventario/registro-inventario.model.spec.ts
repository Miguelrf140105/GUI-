import {
  blankLine,
  changeFamily,
  localDate,
  validateLines,
  selectModel,
  inventoryTotals,
  type ProductLine,
} from './registro-inventario.model';

const line = (patch: Partial<ProductLine> = {}): ProductLine => ({
  ...blankLine(),
  familyId: 1,
  name: 'Laptop Lenovo',
  ...patch,
});

describe('Formulario de inventario', () => {
  it('conserva los campos escritos al buscar y elegir otro tipo', () => {
    const original = line({
      brand: 'Lenovo',
      model: 'E14',
      quantity: 24,
      specifications: { '2': '16' },
    });
    const changed = changeFamily(changeFamily(original, ''), 44);
    expect(changed).toEqual(
      jasmine.objectContaining({
        name: original.name,
        brand: 'Lenovo',
        model: 'E14',
        quantity: 24,
        familyId: 44,
        specifications: {},
      })
    );
    expect(original.specifications).toEqual({ '2': '16' });
  });
  it('desvincula el modelo anterior al cambiar su tipo', () => {
    expect(changeFamily(line({ productId: 636 }), 44).productId).toBe('');
  });
  it('no cambia características si se vuelve a elegir el mismo tipo', () => {
    const original = line({ specifications: { '2': '16' } });
    expect(changeFamily(original, 1)).toBe(original);
  });
  it('admite 24 unidades sin series de fábrica', () =>
    expect(() => validateLines([line({ quantity: 24 })])).not.toThrow());
  it('rechaza cantidades fraccionarias', () =>
    expect(() => validateLines([line({ quantity: 1.5 })])).toThrowError(/enteras/));
  it('rechaza una lista de series incompleta', () =>
    expect(() => validateLines([line({ quantity: 2, serialList: 'SN-1' })])).toThrowError(
      /una\ serie/
    ));
  it('rechaza series repetidas entre distintas líneas', () =>
    expect(() =>
      validateLines([line({ serialList: ' SN-1 ' }), line({ serialList: 'sn-1' })])
    ).toThrowError(/serie/));
  it('rechaza modelos repetidos', () =>
    expect(() => validateLines([line({ productId: 636 }), line({ productId: 636 })])).toThrowError(
      /Agrupa/
    ));
  it('limita el total de toda la recepción', () =>
    expect(() => validateLines([line({ quantity: 600 }), line({ quantity: 500 })])).toThrowError(
      /1000/
    ));
  it('admite el registro sin datos monetarios', () => {
    expect('unitPrice' in blankLine()).toBeFalse();
    expect(() => validateLines([line({ quantity: 24 })])).not.toThrow();
  });
  it('usa la fecha local del equipo', () =>
    expect(localDate(new Date(2026, 9, 22, 23, 30))).toBe('2026-10-22'));
});

describe('Secuencia de registro', () => {
  it('permite reutilizar un modelo conservando cantidad y series', () => {
    expect(
      selectModel(line({ quantity: 24, serialList: 'SN-1' }), {
        id: 636,
        name: 'Monitor',
        brand: 'Gigabyte',
        model: 'GS25F14',
      })
    ).toEqual(
      jasmine.objectContaining({
        productId: 636,
        name: 'Monitor',
        quantity: 24,
        serialList: 'SN-1',
      })
    );
  });
  it('permite volver a un modelo nuevo sin borrar los datos', () => {
    expect(selectModel(line({ productId: 636, brand: 'Lenovo', model: 'E14' }))).toEqual(
      jasmine.objectContaining({
        productId: '',
        name: 'Laptop Lenovo',
        brand: 'Lenovo',
        model: 'E14',
      })
    );
  });
  it('resume modelos distintos en una sola recepción', () => {
    expect(inventoryTotals([line({ quantity: 24 }), line({ quantity: 2 })])).toEqual({
      quantity: 26,
    });
  });
});
