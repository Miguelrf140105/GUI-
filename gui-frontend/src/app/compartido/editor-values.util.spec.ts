import { editorValue } from './editor-values.util';
describe('Valores del editor', () => {
  it('conserva padre y responsable nulos al editar una sede raíz', () => {
    for (const value of [null, undefined, '']) expect(editorValue(value, 'select')).toBeNull();
  });
  it('conserva IDs elegidos y el estado inactivo cero', () => {
    expect(editorValue('201', 'select')).toBe(201);
    expect(editorValue(0, 'select')).toBe(0);
  });
  it('limpia espacios de un nombre sin convertirlo a número', () =>
    expect(editorValue(' UNID ')).toBe('UNID'));
  it('conserva exactamente la contraseña que introduce el usuario', () =>
    expect(editorValue(' Clave con espacios! ', 'password')).toBe(' Clave con espacios! '));
});
