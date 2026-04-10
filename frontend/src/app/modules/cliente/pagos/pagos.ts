import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PagoService } from '../../../core/services/pago.service';

@Component({
  selector: 'app-pagos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pagos.html',
  styleUrls: ['./pagos.css']
})
export class PagosComponent {
  numero = '';
  monto = 0;
  metodo = 1;
  fecha = '';
  mensaje = '';
  pagos: any[] = [];

  constructor(private svc: PagoService) { this.cargar(); }

  cargar() { this.svc.listar().subscribe(list => this.pagos = list || []); }

  registrar() {
    this.mensaje = '';
    if (!this.numero || this.monto <= 0) { this.mensaje = 'Número y monto obligatorios'; return; }
    const payload = { reservacion: this.numero, monto: Number(this.monto), metodo: Number(this.metodo), fecha: this.fecha };
    this.svc.registrar(payload).subscribe({ next: () => { this.mensaje = 'Pago registrado'; this.cargar(); }, error: (e:any) => this.mensaje = e?.error?.mensaje || 'Error' });
  }

  pagosDe(numero: string) { return this.pagos.filter(p => p.numero_reservacion === numero); }
}
