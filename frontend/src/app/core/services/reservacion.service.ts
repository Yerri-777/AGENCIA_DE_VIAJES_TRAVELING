import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Pasajero { nombre?: string; dpi?: string }

export interface NuevaReservacion {
  paquete: { id: number };
  cantidadPasajeros: number;
  fechaViaje: string;
  pasajeros: Pasajero[];
}

@Injectable({ providedIn: 'root' })
export class ReservacionService {
  private readonly BASE = (environment.apiBaseUrl || 'http://localhost:8080/Horizontes').replace(/\/$/, '');

  constructor(private http: HttpClient) {}

  listar(): Observable<any[]> {
    return this.http.get<any[]>(`${this.BASE}/reservaciones`);
  }

  crear(datos: NuevaReservacion) {
    return this.http.post(`${this.BASE}/reservaciones`, datos);
  }

  obtener(numero: string) {
    const params = new HttpParams().set('numero', numero);
    return this.http.get(`${this.BASE}/reservaciones`, { params });
  }

  actualizarEstado(numero: string, estado: string) {
    const params = new HttpParams().set('numero', numero).set('estado', estado);
    return this.http.put(`${this.BASE}/reservaciones`, null, { params });
  }

  // Helpers for related entities
  buscarClientePorDPI(dpi: string): Observable<any> {
    const params = new HttpParams().set('dpi', dpi);
    return this.http.get(`${this.BASE}/clientes`, { params });
  }

  listarPaquetes(): Observable<any[]> {
    return this.http.get<any[]>(`${this.BASE}/paquetes`);
  }

  obtenerPaquete(paqueteId: number): Observable<any> {
    return this.http.get<any>(`${this.BASE}/paquetes?paqueteId=${encodeURIComponent(String(paqueteId))}`);
  }

  crearCliente(payload: any): Observable<any> {
    return this.http.post<any>(`${this.BASE}/clientes`, payload);
  }

  cancelarReservacion(numero: string): Observable<any> {
    return this.http.post<any>(`${this.BASE}/cancelaciones`, { reservacion: numero });
  }
}
