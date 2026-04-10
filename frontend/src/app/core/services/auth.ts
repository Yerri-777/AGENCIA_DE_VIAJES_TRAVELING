import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { tap, map, catchError } from 'rxjs/operators';
import { Usuario } from '../../models/usuario.model';
import { environment } from '../../../environments/environment';
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // Se corrigió la referencia a environment y el reemplazo de la URL
  private readonly AUTH_URL = (environment.apiBaseUrl || 'http://localhost:8080/Horizontes').replace(/\/$/, '') + '/login';
  private usuarioSubject = new BehaviorSubject<Usuario | null>(null);
  public usuario$ = this.usuarioSubject.asObservable();

  constructor(private http: HttpClient) {
    const sesionGuardada = localStorage.getItem('agencia_session');
    if (sesionGuardada) {
      try {
        this.usuarioSubject.next(JSON.parse(sesionGuardada));
      } catch (e) {
        console.error('Error al parsear la sesión guardada', e);
        this.usuarioSubject.next(null);
      }
    }
  }

  login(nombre: string, password: string): Observable<Usuario> {
    const payload = { nombreUsuario: nombre, password };

    return this.http.post<any>(this.AUTH_URL, payload, { withCredentials: true })
      .pipe(
        tap(raw => console.log('[AuthService] raw login response=', raw)),
        map(raw => {
          // LÓGICA DE EXTRACCIÓN DE ROL (Mantenida y reforzada)
          let rolExtraido = 0;
          if (raw.rol && typeof raw.rol === 'object') {
            rolExtraido = Number(raw.rol.id_rol || raw.rol.idRol || 0);
          } else {
            rolExtraido = Number(raw.rol ?? raw.id_rol ?? raw.idRol ?? 0);
          }

          const user: Usuario = {
            nombre: raw?.nombreUsuario || raw?.nombre || nombre,
            rol: rolExtraido,
            idUsuario: raw?.idUsuario || raw?.id_usuario || raw?.id
          };

          // Guardamos en persistencia y notificamos al subject
          localStorage.setItem('agencia_session', JSON.stringify(user));
          this.usuarioSubject.next(user);
          console.log('[AuthService] Login procesado para:', user);

          return user; // Es vital retornar el usuario para que el componente lo reciba
        }),
        catchError(err => {
          console.error('AuthService: login error', err);
          return throwError(() => err);
        })
      );
  }

  logout() {
    localStorage.removeItem('agencia_session');
    this.usuarioSubject.next(null);
  }

  get obtenerRol(): number {
    // Aseguramos que siempre devuelva un número para evitar fallos en los Guards
    return Number(this.usuarioSubject.value?.rol || 0);
  }

  estaLogueado(): boolean {
    return !!this.usuarioSubject.value;
  }
}
