import type { CellValue, Workbook } from 'exceljs';
import { Row } from '../../models/shared.model';

export type ExcelType = 'text' | 'integer' | 'date' | 'datetime';
export interface ExcelColumn {
  key: string;
  label: string;
  excelType?: ExcelType;
}

/** Tipos por contrato, nunca inferidos por el aspecto de un código o documento. */
export function columnExcelType(key: string, moduleId: number): ExcelType {
  if (key === 'date') return moduleId === 120 ? 'datetime' : 'date';
  return [
    'id',
    'quantity',
    'files',
    'documents',
    'permissions',
    'moduleId',
    'actionId',
    'recordId',
  ].includes(key)
    ? 'integer'
    : 'text';
}

export function excelCellValue(value: unknown, type: ExcelType): CellValue {
  if (value === null || value === undefined || value === '') return null;
  if (type === 'text') return String(value);
  if (type === 'integer') {
    if (typeof value === 'string' && !/^-?\d+$/.test(value.trim()))
      throw Error('Se encontró un valor no entero en una columna numérica.');
    const number = Number(value);
    if (!Number.isFinite(number) || !Number.isInteger(number))
      throw Error('Se encontró un valor no entero en una columna numérica.');
    // Excel solo conserva con exactitud 15 dígitos: los identificadores largos se mantienen como texto.
    if (!Number.isSafeInteger(number) || Math.abs(number) >= 1e15) {
      if (typeof value !== 'string')
        throw Error('Un identificador numérico supera la precisión disponible.');
      return value;
    }
    return number;
  }
  const text = value instanceof Date ? value.toISOString() : String(value);
  const match =
    /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2})(?:\.(\d+))?)?(Z|[+-]\d{2}:?\d{2})?)?$/.exec(
      text
    );
  if (!match) throw Error('Se encontró una fecha con formato no válido.');
  const [, year, month, day, hour = '0', minute = '0', second = '0', fraction = '', zone] = match;
  const calendar = new Date(Date.UTC(+year, +month - 1, +day));
  if (
    calendar.getUTCFullYear() !== +year ||
    calendar.getUTCMonth() !== +month - 1 ||
    calendar.getUTCDate() !== +day ||
    +hour > 23 ||
    +minute > 59 ||
    +second > 59
  )
    throw Error('Se encontró una fecha no válida.');
  if (type === 'date') return calendar;
  const date = zone
    ? new Date(text.replace(' ', 'T'))
    : new Date(
        Date.UTC(
          +year,
          +month - 1,
          +day,
          +hour,
          +minute,
          +second,
          Number((fraction + '000').slice(0, 3))
        )
      );
  if (Number.isNaN(date.getTime())) throw Error('Se encontró una fecha no válida.');
  return date;
}

/** Una sola hoja con exactamente las filas y columnas visibles; no consulta otros datos. */
export async function createExcelWorkbook(
  rows: Row[],
  columns: ExcelColumn[],
  title: string,
  moduleId: number
): Promise<Workbook> {
  const ExcelJS = await import('exceljs');
  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'GUI · Inventario UNID';
  workbook.created = new Date();
  const sheet = workbook.addWorksheet(title.replace(/[\\/*?:\[\]]/g, ' ').slice(0, 31) || 'Datos');
  sheet.views = [{ state: 'frozen', ySplit: 1 }];
  sheet.columns = columns.map((column) => ({
    header: column.label,
    key: column.key,
    width: Math.min(42, Math.max(16, column.label.length + 4)),
  }));
  for (const row of rows) {
    sheet.addRow(
      columns.map((column) =>
        excelCellValue(row[column.key], column.excelType || columnExcelType(column.key, moduleId))
      )
    );
  }
  sheet.getRow(1).height = 27;
  sheet.getRow(1).eachCell((cell) => {
    cell.font = { bold: true, color: { argb: 'FFFFFFFF' }, name: 'Calibri', size: 11 };
    cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF24493B' } };
    cell.alignment = { vertical: 'middle', wrapText: true };
  });
  columns.forEach((column, index) => {
    const type = column.excelType || columnExcelType(column.key, moduleId);
    const col = sheet.getColumn(index + 1);
    col.numFmt =
      type === 'text'
        ? '@'
        : type === 'integer'
        ? '0'
        : type === 'date'
        ? 'dd/mm/yyyy'
        : 'dd/mm/yyyy hh:mm:ss';
    let width = Math.max(16, column.label.length + 3);
    for (let row = 2; row <= sheet.rowCount; row++) {
      const cell = sheet.getCell(row, index + 1);
      width = Math.max(width, String(cell.value ?? '').length + 2);
      cell.font = { name: 'Calibri', size: 11, color: { argb: 'FF24493B' } };
      cell.alignment = { vertical: 'top', wrapText: true };
      if (row % 2 === 0)
        cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF2F6ED' } };
      if (typeof cell.value === 'string') cell.numFmt = '@';
    }
    col.width = Math.min(45, width);
  });
  if (columns.length)
    sheet.autoFilter = { from: { row: 1, column: 1 }, to: { row: 1, column: columns.length } };
  sheet.pageSetup = {
    paperSize: 9,
    orientation: 'landscape',
    fitToPage: true,
    fitToWidth: 1,
    fitToHeight: 0,
  };
  return workbook;
}
