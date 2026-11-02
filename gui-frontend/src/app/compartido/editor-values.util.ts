/** Los selectores opcionales vacíos no representan el ID cero. */
export function editorValue(raw: unknown, type?: string): unknown {
  if (raw === null || raw === undefined || raw === '') return null;
  if (['select', 'search', 'number'].includes(type || '')) return Number(raw);
  if (type === 'password') return raw;
  return typeof raw === 'string' ? raw.trim() : raw;
}
