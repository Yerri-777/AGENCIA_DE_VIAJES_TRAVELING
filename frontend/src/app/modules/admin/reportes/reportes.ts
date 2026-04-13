import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReporteService } from '../../../core/services/reporte.service';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.html',
  styleUrls: ['./reportes.css']
})
export class ReportesComponent implements OnInit {

  fechaInicio = '';
  fechaFin = '';
  tipoReporteSeleccionado = 'ventas';


  resumen: any = null;
  error: string | null = null;
  datosReporte: any[] = [];


  reservasDetalle: any[] = [];
  ingresosTotalesBD: number = 0;
  gananciaNetaBD: number = 0;
  cargandoBD: boolean = false;

  constructor(private reporteService: ReporteService) {}

  ngOnInit(): void {
    const hoy = new Date();
    this.fechaFin = hoy.toISOString().split('T')[0];
    const haceUnMes = new Date();
    haceUnMes.setMonth(hoy.getMonth() - 1);
    this.fechaInicio = haceUnMes.toISOString().split('T')[0];

    this.generar();
  }

  generar() {
    this.cargandoBD = true;
    this.notificar(`Sincronizando: ${this.obtenerTitulo(this.tipoReporteSeleccionado)}...`, false);

    this.reporteService.getReport(this.tipoReporteSeleccionado, {
      fechaInicio: this.fechaInicio,
      fechaFin: this.fechaFin
    }).subscribe({
      next: (res: any) => {
        this.procesarDatosHibridos(res);
        this.cargandoBD = false;
      },
      error: () => {
        console.warn("Motor local activado.");
        this.procesarDatosHibridos(null);
        this.cargandoBD = false;
      }
    });
  }

  private procesarDatosHibridos(dataServer: any) {

    const pagosCache = JSON.parse(localStorage.getItem('pagos_cache') || '[]');
    const cancelacionesCache = JSON.parse(localStorage.getItem('cancelaciones_cache') || '[]');
    let reservasCache = JSON.parse(localStorage.getItem('reservas_cache') || '[]');


    reservasCache = reservasCache.filter((r: any) =>
      r.numero && r.numero !== 'S/N' && r.cliente !== 'Desconocido' && Number(r.costo) > 0
    );


    this.reservasDetalle = reservasCache.map((r: any) => ({
      numero: r.numero,
      cliente: r.cliente,
      paquete: r.paquete,
      costo: Number(r.costo || 0),
      estado: r.estado || 'PENDIENTE'
    }));

    const ingresosLocales = pagosCache.reduce((acc: number, p: any) => acc + (Number(p.monto) || 0), 0);
    const reembolsosLocales = cancelacionesCache.reduce((acc: number, c: any) => acc + (Number(c.reembolso) || 0), 0);


    const costosLocales = reservasCache.reduce((acc: number, r: any) => {
      if (r.estado !== 'CANCELADA' && r.estado !== 'REEMBOLSADA') {
        const costoBase = Number(r.costoBaseAgencia) || (Number(r.costo) * 0.7);
        return acc + costoBase;
      }
      return acc;
    }, 0);

    const ventasTotal = (Number(dataServer?.totalVentas) || 0) + ingresosLocales;
    const reembolsosTotal = (Number(dataServer?.totalReembolsos) || 0) + reembolsosLocales;
    const costosTotal = (Number(dataServer?.costosTotales) || 0) + costosLocales;

    this.resumen = {
      totalVentas: ventasTotal,
      totalReembolsos: reembolsosTotal,
      costosTotales: costosTotal,
      gananciaNeta: ventasTotal - costosTotal - reembolsosTotal
    };

    this.ingresosTotalesBD = this.resumen.totalVentas;
    this.gananciaNetaBD = this.resumen.gananciaNeta;


    if (dataServer && dataServer.detalle) {
      this.datosReporte = dataServer.detalle;
    } else {
      this.aplicarLogicaFiltroLocal(this.reservasDetalle, cancelacionesCache);
    }
  }

  private aplicarLogicaFiltroLocal(reservas: any[], cancelaciones: any[]) {
    switch (this.tipoReporteSeleccionado) {
      case 'cancelaciones':
        this.datosReporte = cancelaciones.map(c => ({
          numero: c.numero,
          cliente: c.cliente,
          paquete: `Devolución Efectuada`,
          costo: c.reembolso,
          estado: 'REEMBOLSADA'
        }));
        break;
      case 'ganancias':

        const ventas = reservas.filter(r => r.estado === 'CONFIRMADA').map(r => ({...r, tipo: 'INGRESO'}));
        const gastos = cancelaciones.map(c => ({
          numero: c.numero, cliente: c.cliente, paquete: 'REEMBOLSO', costo: c.reembolso, estado: 'EGRESO'
        }));
        this.datosReporte = [...ventas, ...gastos];
        break;
      default:
        this.datosReporte = [...reservas];
        break;
    }
  }

  exportarPDF() {
    const doc = new jsPDF();
    const fechaDoc = new Date().toLocaleDateString();
    const titulo = this.obtenerTitulo(this.tipoReporteSeleccionado);


    doc.setFillColor(33, 37, 41);
    doc.rect(0, 0, 210, 40, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(22);
    doc.text('HORIZONTES SIN LÍMITES', 14, 22);
    doc.setFontSize(10);
    doc.text(`REPORTE DETALLADO: ${titulo.toUpperCase()}`, 14, 32);
    doc.text(`Fecha: ${fechaDoc}`, 160, 32);


    autoTable(doc, {
      startY: 48,
      head: [['Concepto Financiero', 'Monto Actualizado']],
      body: [
        ['Total Ventas Brutas', `Q${this.resumen.totalVentas.toFixed(2)}`],
        ['Total Reembolsos (Egresos)', `Q${this.resumen.totalReembolsos.toFixed(2)}`],
        ['Costos Operativos Agencia', `Q${this.resumen.costosTotales.toFixed(2)}`],
        ['Utilidad Neta Final', `Q${this.resumen.gananciaNeta.toFixed(2)}`]
      ],
      theme: 'striped',
      headStyles: { fillColor: [41, 128, 185] },
      columnStyles: { 1: { halign: 'right', fontStyle: 'bold' } }
    });


    const finalY = (doc as any).lastAutoTable.finalY || 90;
    doc.setTextColor(0);
    doc.setFontSize(12);
    doc.text('DESGLOSE DE OPERACIONES', 14, finalY + 15);

    autoTable(doc, {
      startY: finalY + 20,
      head: [['Código', 'Cliente', 'Información', 'Estado', 'Monto']],
      body: this.datosReporte.map(d => [
        d.numero,
        d.cliente,
        d.paquete,
        d.estado || 'OK',
        `Q${Number(d.costo).toFixed(2)}`
      ]),
      headStyles: { fillColor: [39, 174, 96] },
      styles: { fontSize: 8 },
      didDrawCell: (data) => {

        if (data.column.index === 3 && (data.cell.raw === 'REEMBOLSADA' || data.cell.raw === 'EGRESO')) {
          doc.setTextColor(231, 76, 60);
        }
      }
    });

    doc.save(`Reporte_Financiero_${this.tipoReporteSeleccionado}.pdf`);
  }

  public obtenerTitulo(tipo: string): string {
    const titulos: any = {
      'ventas': 'Ventas Totales',
      'cancelaciones': 'Reporte de Reembolsos',
      'ganancias': 'Análisis de Utilidades',
      'ocupacion': 'Ocupación por Paquete'
    };
    return titulos[tipo] || 'Reporte de Gestión';
  }

  private notificar(msg: string, err: boolean) {
    this.error = err ? msg : null;
    setTimeout(() => (this.error = null), 4000);
  }
}
