import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReporteService } from '../../../core/services/reporte.service';

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
  resumen: any = null;
  error: string | null = null;

  constructor(private reporteService: ReporteService) {}

  ngOnInit(): void {}

  generar() {
    this.error = null;
    this.resumen = null;
    this.reporteService.generarResumen({ fechaInicio: this.fechaInicio || undefined, fechaFin: this.fechaFin || undefined }).subscribe({
      next: r => this.resumen = r,
      error: e => this.error = 'Error generando reporte'
    });
  }
}

