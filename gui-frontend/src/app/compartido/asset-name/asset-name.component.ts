import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Row } from '../../models/shared.model';
import { InventarioService } from '../../servicios/inventario/inventario.service';
@Component({
  selector: 'gui-asset-name',
  templateUrl: './asset-name.component.html',
  styles: [
    ':host{display:contents}.asset-name-code{padding:14px 16px;background:#f2f6ed;border:1px solid #dfe8d8;border-radius:10px;margin-bottom:20px}.asset-name-code small{display:block;color:#778b7c;font-size:11px;margin-bottom:6px}.asset-name-code strong{font-size:17px;letter-spacing:.5px;color:#24493b}.asset-name-help{font-size:12px;line-height:1.7;color:#76867c;margin:12px 0 0}.asset-name-field{display:flex;flex-direction:column;gap:8px;font-size:12px;color:#4f6a59}',
  ],
})
export class AssetNameComponent implements OnInit {
  @Input() asset: Row = {};
  @Output() saved = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
  @Output() busyChange = new EventEmitter<boolean>();
  name = '';
  busy = false;
  error = '';
  constructor(private inventory: InventarioService) {}
  ngOnInit(): void {
    this.name = this.asset.name || '';
  }
  get canSave(): boolean {
    return (
      !!this.name.trim() && this.name.trim().length <= 200 && this.name.trim() !== this.asset.name
    );
  }
  async submit(): Promise<void> {
    if (this.busy || !this.canSave) return;
    this.busy = true;
    this.error = '';
    this.busyChange.emit(true);
    try {
      await firstValueFrom(
        this.inventory.cambiarNombre(this.asset.id, this.name.trim(), this.asset.name)
      );
      this.saved.emit();
    } catch (error) {
      this.error = (error as Error).message;
    } finally {
      this.busy = false;
      this.busyChange.emit(false);
    }
  }
}
