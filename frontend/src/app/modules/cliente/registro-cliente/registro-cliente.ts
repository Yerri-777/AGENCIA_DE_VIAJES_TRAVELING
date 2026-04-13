import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClienteService, Cliente } from '../../../core/services/cliente.service';
import { OperacionesService } from '../../../core/services/operaciones.service';
import { Router } from '@angular/router';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

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
  // Variables Globales
  vistaActual: string = 'Clientes';
  mensaje: string = '';
  esError: boolean = false;
  saving: boolean = false;

  // datos de clientes
  clientes: Cliente[] = [];
  modelo: Cliente = this.nuevoCliente();
  editando: boolean = false;
  dpiSearch: string = '';
  clienteSeleccionado: any = null;
  showCreateCliente: boolean = false;
  newClienteNombre: string = '';

  // paquetes reservas arreglos
  paquetes: Paquete[] = [];
  selectedPaqueteNombre: string = '';
  fechaViaje: string = '';
  cantidad: number = 1;
  pasajeros: any[] = [{ nombre: '', dpi: '' }];
  estimado: number | null = null;
  resultado: any = null;

  // Caja y Pagos
  numero: string = '';
  monto: number = 0;
  fecha: string = new Date().toISOString().split('T')[0];
  pagos: any[] = [];
  cancelaciones: any[] = [];

  //  Reportes
  reservasDetalle: any[] = [];
  resumen: any = {
    totalVentas: 0,
    gananciaNeta: 0,
    costosTotales: 0,
    totalReembolsos: 0
  };

  constructor(
    private svc: ClienteService,
    private operacionesService: OperacionesService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargar();

   this.operacionesService.paquetes$.subscribe(p => {
  const deCache = JSON.parse(localStorage.getItem('paquetes_cache') || '[]');
  const combinados = [...p, ...deCache];

  const mapa = new Map(combinados.map(paq => {

    const precioNormalizado = paq.precioVenta || paq.precio || 0;
    return [paq.nombre, { ...paq, precioVenta: precioNormalizado }];
  }));

  this.paquetes = Array.from(mapa.values()).filter(paq =>
    (paq.estado !== false) && (paq.capacidad > 0)
  );
});
    this.pagos = JSON.parse(localStorage.getItem('pagos_cache') || '[]');
    this.cancelaciones = JSON.parse(localStorage.getItem('cancelaciones_cache') || '[]');
  }

  // GESTIÓN DE CLIENTES
  cargar() {
    this.svc.listar().subscribe({
      next: (res) => {
        const locales = JSON.parse(localStorage.getItem('clientes_locales') || '[]');
        const unificados = [...res, ...locales];
        const mapa = new Map(unificados.map(c => [c.dpi, c]));
        this.clientes = Array.from(mapa.values());
      },
      error: () => {
        this.notificar('Cargando clientes desde memoria local', true);
        this.clientes = JSON.parse(localStorage.getItem('clientes_locales') || '[]');
      }
    });
  }

  buscar() {
    if (!this.modelo.dpi) return this.notificar('Ingrese un DPI', true);
    const encontrado = this.clientes.find(c => c.dpi === this.modelo.dpi);
    if (encontrado) {
      this.modelo = { ...encontrado };
      this.editando = true;
      this.notificar('Cliente localizado', false);
    } else {
      this.notificar('DPI no registrado en el sistema', false);
    }
  }

  guardar() {
    this.svc.crear(this.modelo).subscribe({
      next: () => {
        alert('Cliente guardado exitosamente');
        this.refrescarYLimpiar();
      },
      error: () => {
        this.svc.crearLocal(this.modelo);
        alert('Guardado en cache local (Servidor no disponible)');
        this.refrescarYLimpiar();
      }
    });
  }

  private refrescarYLimpiar() { this.cargar(); this.limpiar(); }

  // LÓGICA DE RESERVACIONES
  buscarCliente() {
    this.showCreateCliente = false;
    const encontrado = this.clientes.find(c => c.dpi === this.dpiSearch);
    if (encontrado) {
      this.clienteSeleccionado = encontrado;
      this.notificar('Cliente vinculado a la reserva', false);
    } else {
      this.showCreateCliente = true;
    }
  }

  crearCliente() {
    const temp: Cliente = {
      dpi: this.dpiSearch,
      nombreCompleto: this.newClienteNombre,
      fechaNacimiento: '', telefono: '', email: '', nacionalidad: ''
    };
    this.clienteSeleccionado = temp;
    this.svc.crearLocal(temp);
    this.showCreateCliente = false;
    this.cargar();
    this.notificar('Registro temporal creado', false);
  }

  calcularEstimado() {
    const p = this.paquetes.find(x => x.nombre === this.selectedPaqueteNombre);
    if (p) {
      const precio = Number(p.precioVenta) || Number((p as any).precio) || 0;
      this.estimado = precio * this.cantidad;
    } else {
      this.estimado = 0;
    }
  }

  crearReservacion() {
    const p = this.paquetes.find(x => x.nombre === this.selectedPaqueteNombre);
    if (!p || this.cantidad > p.capacidad || !this.fechaViaje) {
      alert("Error: Verifique disponibilidad, paquete y fecha de viaje.");
      return;
    }
    this.saving = true;

    setTimeout(() => {
      const idReserva = 'RES-' + Math.floor(Math.random() * 9000 + 1000);

      this.resultado = {
        numero: idReserva,
        costo: Number(this.estimado) || 0,
        costoBaseAgencia: (Number(p.costoTotalAgencia) || 0) * this.cantidad
      };

      this.operacionesService.actualizarDisponibilidad(p.nombre, this.cantidad);

      const reservas = JSON.parse(localStorage.getItem('reservas_cache') || '[]');
      reservas.push({
        numero: idReserva,
        paquete: p.nombre,
        cliente: this.clienteSeleccionado.nombreCompleto,
        costo: this.resultado.costo,
        costoBaseAgencia: this.resultado.costoBaseAgencia,
        fechaReserva: new Date().toISOString().split('T')[0],
        fechaViaje: this.fechaViaje,
        estado: 'PENDIENTE',
        pagado: 0,
        pasajeros: [...this.pasajeros]
      });
      localStorage.setItem('reservas_cache', JSON.stringify(reservas));

      this.saving = false;
      this.notificar('Reservación confirmada', false);
    }, 1000);
  }

  // CAJA Y PAGOS
  registrar() {
    if (!this.numero || this.monto <= 0) {
      return this.notificar('Ingrese datos válidos de pago', true);
    }

    const reservas = JSON.parse(localStorage.getItem('reservas_cache') || '[]');
    const miReserva = reservas.find((r: any) => (r.numero === this.numero));

    if (miReserva) {
      miReserva.pagado = (Number(miReserva.pagado) || 0) + Number(this.monto);
      if (miReserva.pagado >= miReserva.costo) {
        miReserva.estado = 'CONFIRMADA';
      }
      localStorage.setItem('reservas_cache', JSON.stringify(reservas));
    }

    const nuevoPago = {
      numero_reservacion: this.numero,
      monto: Number(this.monto),
      costoAgencia: miReserva ? Number(miReserva.costoBaseAgencia || 0) : (Number(this.monto) * 0.7),
      fecha: new Date().toLocaleDateString()
    };

    this.pagos.unshift(nuevoPago);
    localStorage.setItem('pagos_cache', JSON.stringify(this.pagos));

    this.notificar('Pago registrado exitosamente', false);
    this.numero = ''; this.monto = 0;
  }

  cancelarSoloReserva(reserva: any) {
  if (confirm(`¿Seguro que desea ANULAR la reserva ${reserva.numero}?`)) {
    this.actualizarEstadoLocal(reserva.numero, 'CANCELADA');
    this.notificar('Reserva marcada como CANCELADA', false);
    this.generar();
  }
}

 procesarCancelacion(reserva: any) {
  if (!reserva || reserva.estado === 'REEMBOLSADA' || reserva.estado === 'CANCELADA') {
    return alert("Esta reserva ya no puede ser procesada.");
  }


  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);

  const fechaV = new Date(reserva.fechaViaje);
  fechaV.setHours(0, 0, 0, 0);

  if (isNaN(fechaV.getTime())) {
    return alert("Error: La fecha de viaje no es válida.");
  }

  // Diferencia en días exactos
  const diffTiempo = fechaV.getTime() - hoy.getTime();
  const diffDias = Math.ceil(diffTiempo / (1000 * 60 * 60 * 24));


  if (diffDias < 7) {
    let mensajeMotivo = diffDias <= 0
      ? "el viaje ya se realizó o es el día de hoy"
      : `solo faltan ${diffDias} días para el viaje`;

    return alert(`Denegado: ${mensajeMotivo}. El sistema requiere un mínimo de 7 días de anticipación para reembolsos.`);
  }

  // Escala de reembolsos
  let pct = 0;
  if (diffDias > 30) pct = 1.0;
  else if (diffDias >= 15) pct = 0.7;
  else if (diffDias >= 7) pct = 0.4;

  const montoPagado = Number(reserva.pagado) || 0;
  const montoDevolver = montoPagado * pct;

  const confirmacion = confirm(
    `ANÁLISIS DE REEMBOLSO:\n` +
    `-----------------------------\n` +
    `Días de anticipación: ${diffDias}\n` +
    `Porcentaje aplicable: ${pct * 100}%\n` +
    `Monto abonado: Q${montoPagado.toFixed(2)}\n` +
    `MONTO A DEVOLVER: Q${montoDevolver.toFixed(2)}\n\n` +
    `¿Desea confirmar el reembolso y anular la reserva?`
  );

  if (confirmacion) {
    this.ejecutarBaja(reserva.numero, 'REEMBOLSADA', montoDevolver, reserva);
  }
}


private ejecutarBaja(numero: string, nuevoEstado: string, montoReembolso: number, reservaOriginal: any) {
  // Actualizar estado en la lista local y persistencia
  const reservas = JSON.parse(localStorage.getItem('reservas_cache') || '[]');
  const idx = reservas.findIndex((r: any) => r.numero === numero);

  if (idx !== -1) {
    reservas[idx].estado = nuevoEstado;
    localStorage.setItem('reservas_cache', JSON.stringify(reservas));

    // Registrar el movimiento en el historial de cancelaciones
    const historial = JSON.parse(localStorage.getItem('cancelaciones_cache') || '[]');
    historial.push({
      numero: numero,
      cliente: reservaOriginal.cliente,
      reembolso: montoReembolso,
      fechaOperacion: new Date().toISOString().split('T')[0],
      estadoPrevio: reservaOriginal.estado
    });
    localStorage.setItem('cancelaciones_cache', JSON.stringify(historial));

    // Devolver el cupo al paquete
    this.operacionesService.actualizarDisponibilidad(reservaOriginal.paquete, -1);

    this.notificar(`Operación ${nuevoEstado} exitosa`, false);
    this.generar();
  }
}
  private actualizarEstadoLocal(numero: string, nuevoEstado: string) {
    const reservas = JSON.parse(localStorage.getItem('reservas_cache') || '[]');
    const idx = reservas.findIndex((r: any) => r.numero === numero);
    if (idx !== -1) {
      reservas[idx].estado = nuevoEstado;
      localStorage.setItem('reservas_cache', JSON.stringify(reservas));
    }
  }

 generar() {
  // Obtener datos de la persistencia
  let reservasC = JSON.parse(localStorage.getItem('reservas_cache') || '[]');
  const pagosC = JSON.parse(localStorage.getItem('pagos_cache') || '[]');
  const cancelC = JSON.parse(localStorage.getItem('cancelaciones_cache') || '[]');

  //
  reservasC = reservasC.filter((r: any) =>
    r.numero &&
    r.numero !== 'S/N' &&
    r.cliente !== 'Desconocido' &&
    (Number(r.costo) > 0 || r.estado === 'CANCELADA')
  );

  localStorage.setItem('reservas_cache', JSON.stringify(reservasC));

  // Mapeo para la tabla de la vista
  this.reservasDetalle = reservasC.map((r: any) => ({
    numero: r.numero,
    cliente: r.cliente,
    paquete: r.paquete,
    costo: Number(r.costo) || 0,
    pagado: Number(r.pagado) || 0,
    estado: r.estado,
    fechaViaje: r.fechaViaje
  }));

  // Variables CAPTURABLES para el Reporte General
  const ingresosCaja = pagosC.reduce((acc: number, p: any) => acc + (Number(p.monto) || 0), 0);
  const totalDevuelto = cancelC.reduce((acc: number, c: any) => acc + (Number(c.reembolso) || 0), 0);
  const totalCostosAgencia = reservasC.reduce((acc: number, r: any) => {

    return (r.estado !== 'CANCELADA') ? acc + (Number(r.costoBaseAgencia) || 0) : acc;
  }, 0);


  this.resumen = {
    totalVentas: ingresosCaja,
    totalReembolsos: totalDevuelto,
    costosTotales: totalCostosAgencia,
    gananciaNeta: ingresosCaja - totalCostosAgencia - totalDevuelto
  };
}

 exportarPDF() {
  const doc = new jsPDF();
  const fechaHoy = new Date().toLocaleDateString();
  const moneda = 'Q';

  doc.setFontSize(18);
  doc.setTextColor(40);
  doc.text('HORIZONTES SIN LÍMITES', 14, 20);

  doc.setFontSize(10);
  doc.setTextColor(100);
  doc.text('Reporte Financiero y de Gestión de Bajas', 14, 25);
  doc.text(`Generado el: ${fechaHoy}`, 14, 30);
  doc.line(14, 32, 196, 32);


  autoTable(doc, {
    startY: 40,
    head: [['Concepto', 'Monto']],
    body: [
      ['Ventas Brutas Totales', `${moneda}${this.resumen.totalVentas.toFixed(2)}`],
      ['Total Egresos por Reembolso', `${moneda}${this.resumen.totalReembolsos.toFixed(2)}`],
      ['Costos Operativos (Agencia)', `${moneda}${this.resumen.costosTotales.toFixed(2)}`],
    ],
    foot: [[
      { content: 'UTILIDAD NETA', styles: { halign: 'right', fontStyle: 'bold' } },
      { content: `${moneda}${this.resumen.gananciaNeta.toFixed(2)}`, styles: { fontStyle: 'bold', textColor: [39, 174, 96] } }
    ]],
    theme: 'grid',
    headStyles: { fillColor: [41, 128, 185] }
  });


  const finalY = (doc as any).lastAutoTable.finalY || 80;
  doc.setFontSize(12);
  doc.setTextColor(0);
  doc.text('DETALLE DE RESERVAS Y ESTADOS DE BAJA', 14, finalY + 15);

  const historialCancelaciones = JSON.parse(localStorage.getItem('cancelaciones_cache') || '[]');

  const filasDetalle = this.reservasDetalle.map(res => {

    const infoCancel = historialCancelaciones.find((c: any) => c.numero === res.numero);
    const montoImpacto = infoCancel ? `-${moneda}${infoCancel.reembolso.toFixed(2)}` : 'Q0.00';

    return [
      res.numero,
      res.cliente,
      res.estado,
      `${moneda}${res.costo.toFixed(2)}`,
      montoImpacto
    ];
  });

  autoTable(doc, {
    startY: finalY + 20,
    head: [['Código', 'Cliente', 'Estado Actual', 'Venta Original', 'Devolución']],
    body: filasDetalle,
    styles: { fontSize: 8 },
    headStyles: { fillColor: [52, 73, 94] },
    didDrawCell: (data) => {
      if (data.section === 'body' && data.column.index === 2) {
        const estado = data.cell.raw;
        if (estado === 'REEMBOLSADA') {
          doc.setTextColor(211, 84, 0);
          doc.setFont('helvetica', 'bold');
        } else if (estado === 'CANCELADA') {
          doc.setTextColor(127, 140, 141);
        } else if (estado === 'CONFIRMADA') {
          doc.setTextColor(39, 174, 96);
        }
      }

      if (data.section === 'body' && data.column.index === 4 && data.cell.raw !== 'Q0.00') {
        doc.setTextColor(231, 76, 60);
      }
    }
  });


  const totalPages = (doc as any).internal.getNumberOfPages();
  for (let i = 1; i <= totalPages; i++) {
    doc.setPage(i);
    doc.setFontSize(8);
    doc.text(`Documento de control interno - Página ${i} de ${totalPages}`, 105, 290, { align: 'center' });
  }

  doc.save(`Reporte_Detallado_${fechaHoy.replace(/\//g, '-')}.pdf`);
}

  irAReservacion(cliente: Cliente) {
    this.clienteSeleccionado = { ...cliente };
    this.dpiSearch = cliente.dpi;
    this.vistaActual = 'Reservaciones';
    this.resultado = null;
    this.estimado = 0;
  }

  irAPagos(res: any) {
    this.numero = res.numero;
    this.monto = res.costo;
    this.vistaActual = 'Pagos';
  }

  salir() { if (confirm('¿Cerrar sesión?')) this.router.navigate(['/login']); }

  limpiar() { this.modelo = this.nuevoCliente(); this.editando = false; }

  nuevoCliente(): Cliente { return { dpi: '', nombreCompleto: '', fechaNacimiento: '', telefono: '', email: '', nacionalidad: '' }; }

  seleccionarParaEditar(c: Cliente) { this.modelo = { ...c }; this.editando = true; }

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

  descargarComprobante() {
    const doc = new jsPDF();
    doc.text(`TICKET DE RESERVA: ${this.resultado.numero}`, 14, 20);
    doc.text(`Cliente: ${this.clienteSeleccionado.nombreCompleto}`, 14, 30);
    doc.text(`Monto: Q${this.resultado.costo.toFixed(2)}`, 14, 40);
    doc.save(`Ticket_${this.resultado.numero}.pdf`);
  }

  private notificar(msg: string, err: boolean) {
    this.mensaje = msg; this.esError = err;
    setTimeout(() => this.mensaje = '', 4000);
  }
}
