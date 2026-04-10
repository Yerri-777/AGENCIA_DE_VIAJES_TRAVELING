import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClienteService, Cliente } from '../../../core/services/cliente.service';
import { OperacionesService } from '../../../core/services/operaciones.service';
import { Router } from '@angular/router';

export interface Paquete {
  nombre: string;
  destino: string;
  duracion: number;
  precioVenta: number;
  capacidad: number;
  servicios: any[];
  estado: boolean;
  costoTotalAgencia?: number;
  gananciaBruta?: number;
}

@Component({
  selector: 'app-registro-cliente',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './registro-cliente.html',
  styleUrls: ['./registro-cliente.css'],
})
export class RegistroClienteComponent implements OnInit {
  // --- Control de Vista ---
  vistaActual: string = 'Clientes';
  mensaje: string = '';
  esError: boolean = false;
  saving: boolean = false;

  // --- Datos de Clientes ---
  clientes: Cliente[] = [];
  modelo: Cliente = this.nuevoCliente();
  editando: boolean = false;

  // --- Datos de Reservaciones (Sincronizados con Rol 2) ---
  paquetes: Paquete[] = [];
  dpiSearch: string = '';
  clienteSeleccionado: any = null;
  showCreateCliente: boolean = false;
  newClienteNombre: string = '';
  selectedPaqueteNombre: string = '';
  cantidad: number = 1;
  pasajeros: any[] = [{ nombre: '', dpi: '' }];
  estimado: number | null = null;
  resultado: any = null;

  // --- Datos de Pagos ---
  numero: string = '';
  monto: number = 0;
  fecha: string = new Date().toISOString().split('T')[0];
  pagos: any[] = [];

  // --- Datos de Reportes ---
  resumen: any = null;

  constructor(
    private svc: ClienteService,
    private operacionesService: OperacionesService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargar();
    this.operacionesService.paquetes$.subscribe(p => {
      this.paquetes = p.filter(paq => paq.estado && (paq.capacidad > 0));
    });
  }

  // --- MÉTODO DE CIERRE DE SESIÓN ---
  salir() {
    if (confirm('¿Desea cerrar sesión y volver al login?')) {
      this.router.navigate(['/login']);
    }
  }

  // --- MÉTODOS DE CLIENTES ---
  nuevoCliente(): Cliente {
    return { dpi: '', nombreCompleto: '', fechaNacimiento: '', telefono: '', email: '', nacionalidad: '' };
  }

  cargar() {
    this.svc.listar().subscribe({
      next: (res) => (this.clientes = res),
      error: () => this.notificar('Error al cargar clientes', true),
    });
  }

  buscar() {
    if (!this.modelo.dpi) return this.notificar('Ingrese un DPI', true);
    this.svc.buscarPorDpi(this.modelo.dpi).subscribe({
      next: (c) => {
        if (c) {
          this.modelo = { ...c };
          this.editando = true;
          this.notificar('Cliente encontrado', false);
        } else {
          this.notificar('Cliente no registrado, puede crearlo abajo', false);
        }
      },
    });
  }

  guardar() {
    this.svc.crear(this.modelo).subscribe({
      next: () => {
        alert('Cliente guardado exitosamente');
        this.cargar();
        this.limpiar();
      },
      error: (err) => alert('Error: ' + (err.error?.mensaje || 'No se pudo guardar'))
    });
  }

  seleccionarParaEditar(c: Cliente) {
    this.modelo = { ...c };
    this.editando = true;
  }

  // --- MÉTODOS DE RESERVACIÓN ---
  buscarCliente() {
    this.showCreateCliente = false;
    this.svc.buscarPorDpi(this.dpiSearch).subscribe({
      next: (c) => {
        if (c) {
          this.clienteSeleccionado = c;
          this.notificar('Cliente vinculado a la reserva', false);
        } else {
          this.showCreateCliente = true;
        }
      },
      error: () => (this.showCreateCliente = true),
    });
  }

  crearCliente() {
    this.clienteSeleccionado = { dpi: this.dpiSearch, nombreCompleto: this.newClienteNombre };
    this.showCreateCliente = false;
    this.notificar('Cliente temporal creado', false);
  }

  calcularEstimado() {
    const p = this.paquetes.find(x => x.nombre === this.selectedPaqueteNombre);
    if (p) {
      this.estimado = p.precioVenta * this.cantidad;
      if (this.cantidad > p.capacidad) {
        this.notificar(`⚠️ Solo quedan ${p.capacidad} cupos`, true);
      }
    } else {
      this.estimado = 0;
    }
  }

  crearReservacion() {
    const p = this.paquetes.find(x => x.nombre === this.selectedPaqueteNombre);
    if (!p || this.cantidad > p.capacidad) {
      alert("Error: No hay cupos suficientes.");
      return;
    }

    this.saving = true;
    setTimeout(() => {
      const idReserva = 'RES-' + Math.floor(Math.random() * 9000 + 1000);
      this.resultado = { numero: idReserva, costo: this.estimado };
      this.operacionesService.actualizarDisponibilidad(p.nombre, this.cantidad);
      this.saving = false;
      this.notificar('Reservación exitosa. Cupo actualizado.', false);
    }, 1000);
  }

  addPassenger() {
    this.pasajeros.push({ nombre: '', dpi: '' });
    this.cantidad = this.pasajeros.length;
    this.calcularEstimado();
  }

  removePassenger(index: number) {
    if (this.pasajeros.length > 1) {
      this.pasajeros.splice(index, 1);
      this.cantidad = this.pasajeros.length;
      this.calcularEstimado();
    }
  }

  registrar() {
    if (!this.numero || this.monto <= 0) {
      return this.notificar('Datos de pago inválidos', true);
    }
    this.pagos.unshift({
      numero_reservacion: this.numero,
      monto: this.monto,
      fecha: new Date().toLocaleDateString()
    });
    this.notificar('Pago registrado en caja', false);
    this.numero = '';
    this.monto = 0;
  }

  generar() {
    this.svc.obtenerReporteReal('resumen').subscribe({
      next: (res: any) => {
        if (res) {
          this.resumen = {
            totalVentas: res.totalVentas || 0,
            gananciaNeta: (res.totalVentas || 0) - (res.totalCancelado || 0)
          };
          this.notificar('Reporte actualizado', false);
        }
      },
      error: () => this.notificar('Error al obtener reporte', true)
    });
  }

  descargarComprobante() {
    if (this.resultado) {
      const url = `http://localhost:8080/Horizontes/reportes?tipo=comprobante&id=${this.resultado.numero}`;
      window.open(url, '_blank');
    }
  }

  exportarPDF() {
    const url = `http://localhost:8080/Horizontes/reportes?tipo=ventas&formato=pdf`;
    window.open(url, '_blank');
  }

  irAReservacion(cliente: Cliente) {
    this.clienteSeleccionado = { ...cliente };
    this.dpiSearch = cliente.dpi;
    this.vistaActual = 'Reservaciones';
    this.resultado = null;
    this.notificar(`Iniciando reserva para ${cliente.nombreCompleto}`, false);
  }

  irAPagos(res: any) {
    this.numero = res.numero;
    this.monto = res.costo;
    this.vistaActual = 'Pagos';
  }

  limpiar() {
    this.modelo = this.nuevoCliente();
    this.editando = false;
  }

  private notificar(msg: string, err: boolean) {
    this.mensaje = msg;
    this.esError = err;
    setTimeout(() => (this.mensaje = ''), 4000);
  }
}
