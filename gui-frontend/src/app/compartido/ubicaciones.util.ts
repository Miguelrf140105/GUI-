import type { Row } from '../models/shared.model';
export function locationPath(id: number, locations: Row[]): string {
  const parts: string[] = [];
  const seen = new Set<number>();
  let current = locations.find((l) => l.id === id);
  while (current && !seen.has(current.id)) {
    seen.add(current.id);
    parts.unshift(current.name);
    current = locations.find((l) => l.id === current!.parentId);
  }
  return parts.join(' / ');
}
export function validParents(id: number | undefined, locations: Row[]): Row[] {
  const excluded = new Set<number>(id ? [id] : []);
  let changed = true;
  while (changed) {
    changed = false;
    for (const l of locations)
      if (excluded.has(l.parentId) && !excluded.has(l.id)) {
        excluded.add(l.id);
        changed = true;
      }
  }
  return locations.filter((l) => !excluded.has(l.id));
}
export function csvText(rows: Row[], columns: { key: string; label: string }[]): string {
  const safe = (value: unknown) => {
    let s = String(value ?? '');
    if (/^[\s]*[=+@-]/.test(s)) s = "'" + s;
    return '"' + s.replaceAll('"', '""') + '"';
  };
  return (
    '\uFEFF' +
    [
      columns.map((c) => safe(c.label)).join(','),
      ...rows.map((r) => columns.map((c) => safe(r[c.key])).join(',')),
    ].join('\r\n')
  );
}
