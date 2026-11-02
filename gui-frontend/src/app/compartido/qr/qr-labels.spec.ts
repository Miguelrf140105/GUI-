import { LABEL_CSS, labelMarkup, labelPages } from './qr-labels';
import { QrComponent } from './qr.component';
describe('Etiquetas estándar individuales y múltiples', () => {
  it('mantiene 35 x 30 mm y distribuye 1, 24, 25 y 49 sin perder activos', () => {
    expect(LABEL_CSS).toContain('width:35mm;height:30mm');
    for (const count of [1, 24, 25, 49]) {
      const values = Array.from({ length: count }, (_, i) => i);
      const pages = labelPages(values);
      expect(pages.length).toBe(Math.ceil(count / 24));
      expect(pages.flat()).toEqual(values);
      expect(pages.every((page) => page.length <= 24)).toBeTrue();
    }
  });
  it('escapa el nombre del activo al preparar la impresión', () => {
    const markup = labelMarkup([
      { code: 'MIC-000001', name: '<script>"&', url: 'local', src: 'data:image/png;base64,AA' },
    ]);
    expect(markup).not.toContain('<script>');
    expect(markup).toContain('&lt;script&gt;&quot;&amp;');
  });
  it('genera un QR distinto por activo y comparte el formato individual', async () => {
    const component = new QrComponent();
    component.assets = [
      { code: 'MIC-1', name: 'Microscopio', token: 'token1' },
      { code: 'MIC-2', name: 'Microscopio', token: 'token2' },
    ];
    await component.ngOnChanges();
    expect(component.labels.length).toBe(2);
    expect(component.labels[0].src).not.toBe(component.labels[1].src);
    expect(component.labels[0].url).toContain('/a/token1');
    component.assets = [];
    component.asset = { code: 'MIC-1', name: 'Microscopio', token: 'token1' };
    await component.ngOnChanges();
    expect(component.labels.length).toBe(1);
    component.ngOnDestroy();
  });
});
