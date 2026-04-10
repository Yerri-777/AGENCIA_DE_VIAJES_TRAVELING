import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReservacionService } from '../../../core/services/reservacion.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-nueva-reservacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './nueva-reservacion.html',
  styleUrls: ['./nueva-reservacion.css']
})
export class NuevaReservacionComponent implements OnInit {
  dpiSearch = '';
  cliente: any = null;
  showCreateCliente = false;
  newClienteNombre = '';
  newClienteTelefono = '';

  paquetes: any[] = [];
  selectedPaqueteId: number | null = null;
  cantidad = 1;
  fechaViaje = '';
  pasajeros: Array<{ nombre?: string; dpi?: string }> = [{ nombre: '', dpi: '' }];

  estimado: number | null = null;
  saving = false;
  resultado: any = null;
  errorMessage: string | null = null;

  private readonly BASE = (environment.apiBaseUrl || 'http://localhost:8080/Horizontes').replace(/\/$/, '');

  constructor(private svc: ReservacionService, private clienteSvc: ClienteService) {}

  ngOnInit(): void {
    this.cargarPaquetes();
  }

  cargarPaquetes() {
    this.svc.listarPaquetes().subscribe({ next: list => this.paquetes = list || [], error: () => this.paquetes = [] });
  }

  buscarCliente() {
    this.cliente = null; this.errorMessage = null; this.showCreateCliente = false;
    if (!this.dpiSearch) { this.errorMessage = 'Ingrese DPI'; return; }
    this.clienteSvc.buscarPorDpi(this.dpiSearch).subscribe({ next: c => { this.cliente = c; }, error: () => { this.showCreateCliente = true; this.errorMessage = 'Cliente no encontrado'; } });
  }

  crearCliente() {
    const payload = { dpi: this.dpiSearch, nombreCompleto: this.newClienteNombre, telefono: this.newClienteTelefono };
    this.clienteSvc.crear(payload).subscribe({ next: () => { this.buscarCliente(); this.showCreateCliente = false; }, error: () => this.errorMessage = 'Error creando cliente' });
  }

  addPassenger() { this.pasajeros.push({ nombre: '', dpi: '' }); }
  removePassenger(i: number) { if (this.pasajeros.length > 1) this.pasajeros.splice(i, 1); }

  calcularEstimado() {
    const p = this.paquetes.find(x => x.id == this.selectedPaqueteId);
    if (!p) { this.estimado = null; return; }
    const precio = Number(p.precio_venta ?? p.precio ?? 0);
    this.estimado = precio * (this.cantidad || 1);
  }

  crearReservacion() {
    this.errorMessage = null;
    if (!this.cliente) { this.errorMessage = 'Busque o cree un cliente primero'; return; }
    if (!this.selectedPaqueteId) { this.errorMessage = 'Seleccione un paquete'; return; }

    const payload = {
      paquete: { id: Number(this.selectedPaqueteId) },
      cantidadPasajeros: Number(this.cantidad),
      fechaViaje: this.fechaViaje,
      pasajeroDpiCliente: this.cliente.dpi,
      pasajeros: this.pasajeros.filter(p => p.dpi)
    };

    this.saving = true;
    this.svc.crear(payload).subscribe({ next: (r:any) => { this.resultado = r; this.saving = false; }, error: () => { this.errorMessage = 'Error al crear la reservación'; this.saving = false; } });
  }

  descargarComprobante() {
    const numero = this.resultado?.numero || this.resultado?.numero_reservacion || this.resultado?.numero;
    if (!numero) return;
    window.open(`${this.BASE}/comprobante?numero=${encodeURIComponent(numero)}`, '_blank');
  }

  cancelarReservacion() {
    const numero = this.resultado?.numero || this.resultado?.numero_reservacion || this.resultado?.numero;
    if (!numero) return;
    this.svc.cancelarReservacion(numero).subscribe({ next: () => { this.resultado = null; alert('Cancelación procesada'); }, error: () => { this.errorMessage = 'Error al cancelar'; } });
  }
}
