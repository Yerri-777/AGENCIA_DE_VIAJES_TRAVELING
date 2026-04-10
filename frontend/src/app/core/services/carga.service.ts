import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RespuestaCarga {
  destinos: any;
  proveedores: any;
  procesadas: number;
  errores: number;
  mensaje: string;
  detalles?: string[];
}

@Injectable({ providedIn: 'root' })
export class CargaService {
  private readonly URL = (environment.apiBaseUrl || 'http://localhost:8080/Horizontes').replace(/\/$/, '') + '/carga';

  constructor(private http: HttpClient) {}

  uploadFile(file: File): Observable<HttpEvent<RespuestaCarga>> {
    const fd = new FormData();

    fd.append('archivo', file, file.name);

    return this.http.post<RespuestaCarga>(this.URL, fd, {
      withCredentials: false,
      reportProgress: true,
      observe: 'events'
    });
  }
}
