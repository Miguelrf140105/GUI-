import { Row } from '../../models/shared.model';

export const LABELS_PER_PAGE = 24;
export function labelPages<T>(labels: T[]): T[][] {
  return Array.from({ length: Math.ceil(labels.length / LABELS_PER_PAGE) }, (_, i) =>
    labels.slice(i * LABELS_PER_PAGE, (i + 1) * LABELS_PER_PAGE)
  );
}
export const LABEL_CSS = `
.qr-sheet{display:grid;grid-template-columns:repeat(4,35mm);grid-auto-rows:30mm;gap:4mm;align-content:start;justify-content:center;box-sizing:border-box;break-after:page;page-break-after:always;margin:0 auto 8mm}
.qr-sheet:last-child{break-after:auto;page-break-after:auto}
.qr-label{box-sizing:border-box;width:35mm;height:30mm;overflow:hidden;border:.2mm solid #dce5d2;border-radius:1mm;background:white;color:#24493b;display:flex;flex-direction:column;align-items:center;padding:.8mm;font-family:Arial,sans-serif;break-inside:avoid}
.qr-label-brand{height:3mm;display:flex;align-items:center;gap:.7mm;flex-shrink:0}
.qr-label-brand b{font-size:6pt}.qr-label-brand span{font-size:2.7pt;color:#879d78;line-height:1.1;max-width:10mm}
.qr-label-brand svg{width:1.7mm;height:2.2mm}
.qr-label img{display:block;width:21mm;height:21mm;flex-shrink:0;margin:0;image-rendering:pixelated}
.qr-label-code{font-size:6pt;line-height:2.3mm;letter-spacing:.15mm;font-weight:bold;white-space:nowrap}
.qr-label-name{font-size:3.6pt;line-height:1.8mm;max-width:32mm;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
@media print{ @page{size:A4 portrait;margin:10mm} html,body{margin:0;padding:0} .qr-sheet{margin:0 auto;height:270mm} *{print-color-adjust:exact;-webkit-print-color-adjust:exact} }
`;
export interface QrLabel {
  code: string;
  name: string;
  url: string;
  src: string;
}
export function validLabelAsset(asset: Row): boolean {
  return !!asset.code && !!asset.token && !!asset.name;
}
export function labelMarkup(labels: QrLabel[]): string {
  const escape = (value: string) =>
    value.replace(
      /[&<>"']/g,
      (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string)
    );
  return labelPages(labels)
    .map(
      (page) =>
        `<section class="qr-sheet">${page
          .map(
            (label) => `
    <article class="qr-label"><div class="qr-label-brand">
    <svg viewBox="0 0 16 24" fill="none" stroke="currentColor" stroke-width="1.4"><path d="M3 22V2h10v20M1 22h14M6 5h1m2 0h1M6 9h1m2 0h1M6 13h1m2 0h1M6 17h1m2 0h1"/></svg>
    <b>GUI</b><span>Patrimonio<br>universitario</span></div>
    <img src="${escape(label.src)}" alt="QR de ${escape(label.code)}">
    <div class="qr-label-code">${escape(label.code)}</div>
    <div class="qr-label-name">${escape(label.name)}</div></article>`
          )
          .join('')}</section>`
    )
    .join('');
}
