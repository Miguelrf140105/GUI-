import {
  Component,
  ElementRef,
  EventEmitter,
  OnDestroy,
  Input,
  Output,
  ViewChild,
} from '@angular/core';
import jsQR from 'jsqr';

@Component({
  selector: 'gui-qr-scanner',
  templateUrl: './qr-scanner.component.html',
  styles: [
    'video{display:block;width:100%;max-height:360px;background:#142e2b;border-radius:12px;margin:12px 0}',
  ],
})
export class QrScannerComponent implements OnDestroy {
  @Input() continuous = false;
  private lastRead = '';
  private lastReadAt = 0;
  @Output() read = new EventEmitter<string>();
  @ViewChild('preview', { static: true }) preview!: ElementRef<HTMLVideoElement>;
  running = false;
  starting = false;
  error = '';
  private stream?: MediaStream;
  private timer?: ReturnType<typeof setInterval>;
  private generation = 0;
  private detecting = false;
  private canvas = document.createElement('canvas');
  async start(): Promise<void> {
    if (this.running || this.starting) return;
    if (!navigator.mediaDevices?.getUserMedia) {
      this.error =
        'La cámara no está disponible. Abre GUI en localhost o por HTTPS, o introduce el QR en Inventario.';
      return;
    }
    this.error = '';
    this.starting = true;
    const generation = ++this.generation;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' } },
        audio: false,
      });
      if (generation !== this.generation) {
        stream.getTracks().forEach((track) => track.stop());
        return;
      }
      this.stream = stream;
      this.preview.nativeElement.srcObject = stream;
      await this.preview.nativeElement.play();
      if (generation !== this.generation) return;
      this.running = true;
      this.timer = setInterval(() => {
        if (this.detecting || this.preview.nativeElement.readyState < 2) return;
        this.detecting = true;
        try {
          const value = this.decodeFrame();
          if (generation === this.generation && value) {
            const now = Date.now();
            if (!this.continuous || value !== this.lastRead || now - this.lastReadAt >= 1500) {
              this.lastRead = value;
              this.lastReadAt = now;
              if (!this.continuous) this.stop();
              this.read.emit(value);
            }
          }
        } catch {
          /* Un cuadro aún no disponible se vuelve a intentar. */
        } finally {
          this.detecting = false;
        }
      }, 250);
    } catch (error) {
      if (generation !== this.generation) return;
      this.stop();
      this.error =
        (error as Error).name === 'NotAllowedError'
          ? 'No se autorizó la cámara. Puedes permitirla en el navegador o introducir el QR manualmente.'
          : 'No se pudo abrir la cámara. Comprueba que esté conectada y no la use otra aplicación.';
    } finally {
      if (generation === this.generation) this.starting = false;
    }
  }
  decodeFrame(): string | null {
    const video = this.preview.nativeElement;
    if (!video.videoWidth || !video.videoHeight) return null;
    const width = Math.min(video.videoWidth, 960);
    const height = Math.round((video.videoHeight * width) / video.videoWidth);
    if (this.canvas.width !== width) this.canvas.width = width;
    if (this.canvas.height !== height) this.canvas.height = height;
    const context = this.canvas.getContext('2d');
    if (!context) return null;
    context.drawImage(video, 0, 0, width, height);
    const pixels = context.getImageData(0, 0, width, height);
    return jsQR(pixels.data, width, height)?.data ?? null;
  }
  stop(): void {
    this.generation++;
    if (this.timer) clearInterval(this.timer);
    this.timer = undefined;
    this.stream?.getTracks().forEach((track) => track.stop());
    this.stream = undefined;
    if (this.preview) this.preview.nativeElement.srcObject = null;
    this.running = false;
    this.starting = false;
  }
  ngOnDestroy(): void {
    this.stop();
  }
}
