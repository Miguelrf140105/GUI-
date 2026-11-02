import { Workbook, ValueType } from 'exceljs';
import { createExcelWorkbook, excelCellValue } from './excel-export';
import { ListadoComponent } from './listado.component';

describe('Exportación Excel tipada', () => {
  it('conserva texto, enteros, fechas y vacíos después de escribir y volver a abrir el XLSX', async () => {
    const columns = [
      { key: 'code', label: 'Código' },
      { key: 'serial', label: 'Serie' },
      { key: 'document', label: 'RUC' },
      { key: 'quantity', label: 'Cantidad' },
      { key: 'date', label: 'Fecha' },
      { key: 'detail', label: 'Detalle' },
    ];
    const source = [
      {
        code: 'MIC-000001',
        serial: '00000123',
        document: '00123456789',
        quantity: 24,
        date: '2026-10-26',
        detail: null,
      },
    ];
    const workbook = await createExcelWorkbook(source, columns, 'Inventario', 30);
    const bytes = await workbook.xlsx.writeBuffer();
    const opened = new Workbook();
    await opened.xlsx.load(bytes);
    const sheet = opened.worksheets[0];
    expect(sheet.getCell('A2').value).toBe('MIC-000001');
    expect(sheet.getCell('B2').type).toBe(ValueType.String);
    expect(sheet.getCell('B2').value).toBe('00000123');
    expect(sheet.getCell('C2').value).toBe('00123456789');
    expect(sheet.getCell('D2').type).toBe(ValueType.Number);
    expect(sheet.getCell('D2').value).toBe(24);
    expect(sheet.getCell('E2').type).toBe(ValueType.Date);
    expect((sheet.getCell('E2').value as Date).toISOString()).toBe('2026-10-26T00:00:00.000Z');
    expect(sheet.getCell('E2').numFmt).toBe('dd/mm/yyyy');
    expect(sheet.getCell('F2').value).toBeNull();
    expect(sheet.rowCount).toBe(2);
    expect(sheet.columnCount).toBe(6);
  });
  it('trata los textos que comienzan con = como texto, no como fórmulas', async () => {
    const workbook = await createExcelWorkbook(
      [{ name: '=1+1' }],
      [{ key: 'name', label: 'Producto' }],
      'Datos',
      30
    );
    const reopened = new Workbook();
    await reopened.xlsx.load(await workbook.xlsx.writeBuffer());
    expect(reopened.worksheets[0].getCell('A2').type).toBe(ValueType.String);
    expect(reopened.worksheets[0].getCell('A2').value).toBe('=1+1');
  });
  it('conserva la fecha UTC y los identificadores que exceden la precisión de Excel', () => {
    expect((excelCellValue('2026-10-26T15:10:00', 'datetime') as Date).toISOString()).toBe(
      '2026-10-26T15:10:00.000Z'
    );
    expect(excelCellValue('12345678901234567', 'integer')).toBe('12345678901234567');
    expect(() => excelCellValue('2026-02-30', 'date')).toThrowError();
    expect(() => excelCellValue('24.5', 'integer')).toThrowError();
  });
  it('el botón descarga exclusivamente la búsqueda y los filtros de columna de la página actual', async () => {
    const list = new ListadoComponent({} as any, {} as any, {} as any, {} as any);
    list.result.rows = [
      { id: 1, code: 'MIC-000001', name: 'Microscopio', family: 'Microscopio', serial: '0001' },
      { id: 2, code: 'MIC-000002', name: 'Microscopio', family: 'Microscopio' },
      { id: 3, code: 'MON-000001', name: 'Monitor', family: 'Monitor' },
    ];
    list.query = 'Microscopio';
    list.columnQueries = { code: '000001' };
    let exported: Blob | undefined;
    spyOn(URL, 'createObjectURL').and.callFake((blob: any) => {
      exported = blob;
      return 'blob:excel-test';
    });
    spyOn(URL, 'revokeObjectURL').and.stub();
    spyOn(HTMLAnchorElement.prototype, 'click').and.stub();
    await list.exportRows();
    expect(list.error).toBe('');
    expect(exported!.type).toBe(
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    );
    const reopened = new Workbook();
    await reopened.xlsx.load(await exported!.arrayBuffer());
    const sheet = reopened.worksheets[0];
    expect(sheet.rowCount).toBe(2);
    expect(sheet.getCell('A2').value).toBe('MIC-000001');
    expect(sheet.getRow(1).values).not.toContain('Acciones');
    expect(sheet.getRow(1).values).not.toContain('ID del activo');
  });
  it('no descarga un archivo cuando la búsqueda no tiene coincidencias', async () => {
    const list = new ListadoComponent({} as any, {} as any, {} as any, {} as any);
    const create = spyOn(URL, 'createObjectURL');
    await list.exportRows();
    expect(create).not.toHaveBeenCalled();
  });
});
