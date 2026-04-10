import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

export interface PagoRequest {
  reservacion: string;
  monto: number;
  metodo: number; // 1: Efectivo, 2: Tarjeta, 3: Transferencia
  fecha?: string;
}

export interface Pago {
  id_pago: number;
  monto: number;
  metodo_pago: number;
  fecha_pago: string;
  numero_reservacion: string;
  id_usuario: number;
}

@Injectable({ providedIn: 'root' })
export class PagoService {
  private readonly URL = (environment.apiBaseUrl || 'http://localhost:8080/Horizontes').replace(/\/$/, '') + '/pagos';

  constructor(private http: HttpClient) {}

  listar(): Observable<Pago[]> {
    return this.http.get<Pago[]>(this.URL);
  }

  registrar(p: PagoRequest) {
    return this.http.post(this.URL, p);
  }
}
