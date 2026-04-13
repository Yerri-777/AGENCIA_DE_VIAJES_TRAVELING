import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface Cliente {
  dpi: string;
  nombreCompleto?: string;
  fechaNacimiento?: string;
  telefono?: string;
  email?: string;
  nacionalidad?: string;
}

@Injectable({ providedIn: 'root' })
export class ClienteService {
  crearLocal(payload: { dpi: string; nombre: string; nacimiento: string; tel: string; email: string; nacionalidad: string; } | Cliente) {

    const cliente: Cliente = {
      dpi: (payload as any).dpi || '',
      nombreCompleto: (payload as any).nombreCompleto || (payload as any).nombre || '',
      fechaNacimiento: (payload as any).fechaNacimiento || (payload as any).nacimiento || '',
      telefono: (payload as any).telefono || (payload as any).tel || '',
      email: (payload as any).email || '',
      nacionalidad: (payload as any).nacionalidad || ''
    };


    try {
      const key = 'clientes_cache';
      const list = JSON.parse(localStorage.getItem(key) || '[]');
      list.push(cliente);
      localStorage.setItem(key, JSON.stringify(list));
    } catch (e) {
      console.warn('No se pudo guardar cliente en localStorage', e);
    }


    try {
      this.crear(cliente).subscribe({
        next: () => console.log('Cliente creado en backend:', cliente.dpi),
        error: () => console.warn('No se pudo crear cliente en backend, quedó en cache local')
      });
    } catch (e) {

    }
  }
  private readonly base = (environment.apiBaseUrl || 'http://localhost:8080/Horizontes').replace(/\/$/, '') + '/';
  private readonly URL = this.base + 'clientes';

  obtenerReporteReal(tipo: string): Observable<any> {
    if (tipo === 'resumen') {
      const ventas$ = this.http.get<any[]>(this.base + 'reportes?tipo=ventas');
      const cancel$ = this.http.get<any[]>(this.base + 'reportes?tipo=cancelaciones');
      return forkJoin([ventas$, cancel$]).pipe(
        map(([ventas, cancelaciones]) => {
          const totalVentas = (ventas || []).reduce((s, r) => s + (Number(r.costo_total) || 0), 0);
          const totalCancelado = (cancelaciones || []).reduce((s, r) => s + (Number(r.monto_reembolso) || 0), 0);
          return { totalVentas, totalCancelado };
        })
      );
    }
    return this.http.get<any>(this.base + 'reportes?tipo=' + encodeURIComponent(tipo));
  }

  constructor(private http: HttpClient) {}

  listar(): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(this.URL);
  }

  buscarPorDpi(dpi: string): Observable<Cliente> {
    return this.http.get<Cliente>(this.URL + '?dpi=' + encodeURIComponent(dpi));
  }

  crear(cliente: Cliente) {
    return this.http.post(this.URL, cliente);
  }

  actualizar(cliente: Cliente) {
    return this.http.put(this.URL, cliente);
  }

  eliminar(dpi: string) {
    return this.http.delete(this.URL + '?dpi=' + encodeURIComponent(dpi));
  }
}
