import { Component, Input, OnChanges, OnDestroy } from '@angular/core';
import * as QRCode from 'qrcode';
import { Row } from '../../models/shared.model';
import { LABEL_CSS, QrLabel, labelPages, labelMarkup, validLabelAsset } from './qr-labels';
@Component({
  selector: 'gui-qr',
  templateUrl: './qr.component.html',
  styles: [
    ':host { display: contents; } .qr-preview{overflow:auto;max-height:55vh;padding:8px} ' +
      LABEL_CSS,
  ],
})
export class QrComponent implements OnChanges, OnDestroy {
  @Input() asset: Row = {};
  @Input() assets: Row[] = [];
  labels: QrLabel[] = [];
  error = '';
  loading = false;
  printing = false;
  private generation = 0;
  private frame?: HTMLIFrameElement;
  pages: QrLabel[][] = [];
  async ngOnChanges(): Promise<void> {
    const generation = ++this.generation;
    this.labels = [];
    this.pages = [];
    this.error = '';
    this.loading = true;
    try {
      const assets = this.assets.length ? this.assets : [this.asset];
      if (!assets.every(validLabelAsset))
        throw Error('Faltan datos de identificación de una etiqueta. Actualiza el inventario.');
      const labels: QrLabel[] = [];
      for (const asset of assets) {
        const url = new URL('/a/' + asset.token, window.location.origin).toString();
        const src = await QRCode.toDataURL(url, {
          width: 400,
          margin: 4,
          color: { dark: '#102d3a', light: '#ffffff' },
          errorCorrectionLevel: 'M',
        });
        labels.push({ code: asset.code, name: asset.name, url, src });
      }
      if (generation === this.generation) {
        this.labels = labels;
        this.pages = labelPages(labels);
      }
    } catch (error) {
      if (generation === this.generation) this.error = (error as Error).message;
    } finally {
      if (generation === this.generation) this.loading = false;
    }
  }
  async print(): Promise<void> {
    if (!this.labels.length || this.loading || this.printing) return;
    this.printing = true;
    this.error = '';
    this.frame?.remove();
    const frame = document.createElement('iframe');
    frame.title = 'Impresión de etiquetas QR';
    frame.style.cssText = 'position:fixed;left:-10000px;top:0;width:210mm;height:297mm;border:0';
    this.frame = frame;
    document.body.appendChild(frame);
    try {
      const doc = frame.contentDocument!;
      doc.open();
      doc.write(
        '<!doctype html><html><head><meta charset="utf-8"><title>GUI - Etiquetas QR</title><style>' +
          LABEL_CSS +
          '</style></head><body>' +
          labelMarkup(this.labels) +
          '</body></html>'
      );
      doc.close();
      await Promise.all(Array.from(doc.images).map((image) => image.decode()));
      if (this.frame !== frame) return;
      frame.contentWindow!.focus();
      frame.contentWindow!.print();
    } catch {
      this.error = 'No se pudo abrir la impresión. Inténtalo de nuevo.';
    } finally {
      this.printing = false;
    }
  }
  ngOnDestroy(): void {
    this.generation++;
    this.frame?.remove();
  }
}
