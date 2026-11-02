import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { InventarioService } from '../../servicios/inventario/inventario.service';
import { Row } from '../../models/shared.model';
@Component({
  selector: 'gui-qr-route',
  templateUrl: './qr-route.component.html',
  styles: [':host { display: contents; }'],
})
export class QrRouteComponent implements OnInit {
  asset: Row | null = null;
  error = '';
  qr = false;
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: InventarioService
  ) {}
  async ngOnInit(): Promise<void> {
    try {
      this.asset = await firstValueFrom(
        this.api.qr(this.route.snapshot.paramMap.get('token') || '')
      );
    } catch (error) {
      this.error = (error as Error).message;
    }
  }
  close(): void {
    this.router.navigate(['/inventario']);
  }
}
