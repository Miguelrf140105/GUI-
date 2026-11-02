// Las filas SQL son proyecciones de lectura. Los formularios usan contratos DTO en el backend.
export type Row = Record<string, any>;
export type Session = { id: number; name: string; username: string; permissions: number[] };
export type Page = { rows: Row[]; page: number; size: number; hasMore: boolean };
export type Lookups = Record<string, Row[]>;
