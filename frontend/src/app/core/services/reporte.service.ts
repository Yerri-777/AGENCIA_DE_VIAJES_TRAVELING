import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private base = environment.apiBaseUrl;
  constructor(private http: HttpClient) {}

  generarResumen(filtro: { fechaInicio?: string; fechaFin?: string }): Observable<any> {
    return this.http.post<any>(this.base + 'reportes', filtro, { withCredentials: true });
  }

  getReport(tipo: string, params?: { fechaInicio?: string; fechaFin?: string }): Observable<any> {
    let url = this.base + 'reportes?tipo=' + encodeURIComponent(tipo);
    if (params?.fechaInicio) url += '&fechaInicio=' + encodeURIComponent(params.fechaInicio);
    if (params?.fechaFin) url += '&fechaFin=' + encodeURIComponent(params.fechaFin);
    return this.http.get<any>(url, { withCredentials: true });
  }
}
