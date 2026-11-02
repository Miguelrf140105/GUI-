import { Component, Input, OnInit } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { DocumentosService } from '../../servicios/documentos/documentos.service';
import { Row } from '../../models/shared.model';
@Component({
  selector: 'gui-archivos',
  templateUrl: './archivos.component.html',
  styles: [':host { display: contents; }'],
})
export class ArchivosComponent implements OnInit {
  @Input() parent: { documentId?: number; assetId?: number } = {};
  @Input() canUpload = false;
  files: Row[] = [];
  error = '';
  busy = false;
  constructor(public api: DocumentosService) {}
  ngOnInit(): void {
    this.load();
  }
  async load(): Promise<void> {
    try {
      this.files = await firstValueFrom(this.api.listar(this.parent));
    } catch (error) {
      this.error = (error as Error).message;
    }
  }
  async upload(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.error = '';
    if (file.size > 10485760) {
      this.error = 'El archivo supera 10 MB.';
      input.value = '';
      return;
    }
    this.busy = true;
    try {
      await firstValueFrom(this.api.subir(this.parent, file));
      await this.load();
    } catch (error) {
      this.error = (error as Error).message;
    } finally {
      this.busy = false;
      input.value = '';
    }
  }
}
