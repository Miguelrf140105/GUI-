import { fakeAsync, tick } from '@angular/core/testing';
import { QrScannerComponent } from './qr-scanner.component';
import * as QRCode from 'qrcode';
describe('Cámara QR', () => {
  it('detiene la cámara al cerrar aunque la autorización responda después', async () => {
    let resolve!: (stream: MediaStream) => void;
    spyOn(navigator.mediaDevices, 'getUserMedia').and.returnValue(
      new Promise((r) => (resolve = r))
    );
    const stop = jasmine.createSpy();
    const c = new QrScannerComponent();
    c.preview = { nativeElement: { srcObject: null, play: () => Promise.resolve() } as any };
    const opening = c.start();
    c.ngOnDestroy();
    resolve({ getTracks: () => [{ stop }] } as any);
    await opening;
    expect(stop).toHaveBeenCalled();
    expect(c.running).toBeFalse();
  });
  it('lee el contenido del QR una vez y libera todos los tracks', fakeAsync(() => {
    const stop = jasmine.createSpy();
    spyOn(navigator.mediaDevices, 'getUserMedia').and.returnValue(
      Promise.resolve({ getTracks: () => [{ stop }] } as any)
    );
    const c = new QrScannerComponent();
    c.preview = {
      nativeElement: { srcObject: null, readyState: 2, play: () => Promise.resolve() } as any,
    };
    spyOn(c.read, 'emit');
    spyOn(c, 'decodeFrame').and.returnValue('http://localhost:4200/a/token');
    try {
      void c.start();
      tick();
      tick(250);
      expect(c.read.emit).toHaveBeenCalledOnceWith('http://localhost:4200/a/token');
      expect(stop).toHaveBeenCalled();
      expect(c.running).toBeFalse();
    } finally {
      c.ngOnDestroy();
    }
  }));
  it('decodifica un QR real generado localmente sin servicios externos', async () => {
    const canvas = document.createElement('canvas');
    const value = 'http://localhost:4200/a/00000000-0000-0000-0000-000000000001';
    await QRCode.toCanvas(canvas, value, { width: 300 });
    (canvas as any).videoWidth = canvas.width;
    (canvas as any).videoHeight = canvas.height;
    const c = new QrScannerComponent();
    c.preview = { nativeElement: canvas as any };
    expect(c.decodeFrame()).toBe(value);
    c.ngOnDestroy();
  });
});

describe('Escaneo continuo', () => {
  it('lee dos QR sin cerrar la cámara y frena las repeticiones del mismo cuadro', fakeAsync(() => {
    const stop = jasmine.createSpy('stop');
    spyOn(navigator.mediaDevices, 'getUserMedia').and.returnValue(
      Promise.resolve({ getTracks: () => [{ stop }] } as any)
    );
    const c = new QrScannerComponent();
    c.continuous = true;
    c.preview = {
      nativeElement: { srcObject: null, readyState: 2, play: () => Promise.resolve() } as any,
    };
    const decode = spyOn(c, 'decodeFrame').and.returnValue('QR-1');
    const emit = spyOn(c.read, 'emit');
    try {
      void c.start();
      tick();
      tick(250);
      tick(500);
      expect(emit).toHaveBeenCalledTimes(1);
      expect(c.running).toBeTrue();
      expect(stop).not.toHaveBeenCalled();
      decode.and.returnValue('QR-2');
      tick(250);
      expect(emit).toHaveBeenCalledTimes(2);
    } finally {
      c.ngOnDestroy();
    }
    expect(stop).toHaveBeenCalled();
  }));
});
