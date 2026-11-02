/** Reglas de presentación del formulario. Los permisos y las reglas de negocio se validan en el backend. */
export type ProductLine = {
  key: string;
  familyId: number | '';
  productId: number | '';
  name: string;
  brand: string;
  model: string;
  quantity: number | '';
  serialList: string;
  specifications: Record<string, string | number | null>;
};

export function localDate(date = new Date()): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate()
  ).padStart(2, '0')}`;
}

export function blankLine(): ProductLine {
  return {
    key: crypto.randomUUID(),
    familyId: '',
    productId: '',
    name: '',
    brand: '',
    model: '',
    quantity: 1,
    serialList: '',
    specifications: {},
  };
}

/** Buscar otro tipo no debe borrar nombre, marca, modelo ni cantidad ingresados a mano. */
export function changeFamily(line: ProductLine, familyId: number | ''): ProductLine {
  if (familyId === line.familyId) return line;
  return { ...line, familyId, productId: '', specifications: {} };
}

export function validateLines(lines: ProductLine[]): void {
  const models = new Set<number>();
  const serials = new Set<string>();
  let total = 0;
  for (const [index, line] of lines.entries()) {
    const product = `Producto ${index + 1}`;
    if (!line.familyId) throw Error(`${product}: selecciona un tipo de la lista.`);
    if (!line.productId && !line.name.trim()) throw Error(`${product}: escribe el nombre.`);
    if (
      !Number.isInteger(line.quantity) ||
      Number(line.quantity) < 1 ||
      Number(line.quantity) > 1000
    )
      throw Error(`${product}: indica entre 1 y 1000 unidades enteras.`);
    if (line.productId) {
      if (models.has(line.productId))
        throw Error('Agrupa las unidades del mismo modelo en una sola línea.');
      models.add(line.productId);
    }
    const values = line.serialList.trim()
      ? line.serialList.split(/\r?\n/).map((value) => value.trim())
      : [];
    if (values.length && values.length !== line.quantity)
      throw Error(`${product}: indica una serie por unidad o deja la lista vacía.`);
    for (const value of values) {
      if (!value) continue;
      const serial = value.toLocaleUpperCase('es');
      if (serials.has(serial)) throw Error('No repitas números de serie entre los productos.');
      serials.add(serial);
    }
    total += Number(line.quantity);
  }
  if (!lines.length || total > 1000) throw Error('Registra entre 1 y 1000 unidades por recepción.');
}

/** El selector reutiliza una referencia; elegir Nuevo conserva lo escrito para poder ajustarlo. */
export function selectModel(
  line: ProductLine,
  model?: { id?: number; name?: string; brand?: string; model?: string }
): ProductLine {
  if (!model?.id) return { ...line, productId: '' };
  return {
    ...line,
    productId: model.id,
    name: model.name || '',
    brand: model.brand || '',
    model: model.model || '',
  };
}

/** Resumen visual; no reserva correlativos ni sustituye la validación del servidor. */
export function inventoryTotals(lines: ProductLine[]): { quantity: number } {
  return { quantity: lines.reduce((total, line) => total + (Number(line.quantity) || 0), 0) };
}
