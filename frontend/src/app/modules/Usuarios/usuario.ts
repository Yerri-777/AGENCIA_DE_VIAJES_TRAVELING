import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';

interface Usuario {
  username: string;
  nombre: string;
  rol: 'AGENTE' | 'OPERADOR' | 'ADMIN';
  estado: 'ACTIVO' | 'INACTIVO';
  origen: 'DB' | 'LOCAL';
}

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './usuarios.html',
  styleUrls: ['./usuario.css']
})
export class UsuariosComponent implements OnInit {
  usuarios: Usuario[] = [];
  cargando: boolean = false;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.cargarUsuarios();
  }

  cargarUsuarios(): void {
    this.cargando = true;


    const localesRaw = JSON.parse(localStorage.getItem('usuarios_sistema') || '[]');
    const locales: Usuario[] = localesRaw.map((u: any) => ({
      username: u.username,
      nombre: u.nombre || 'Personal Externo',
      rol: u.rol === 1 ? 'AGENTE' : (u.rol === 2 ? 'OPERADOR' : 'ADMIN'),
      estado: u.estado || 'ACTIVO',
      origen: 'LOCAL'
    }));


    this.http.get<any[]>('http://localhost:8080/Horizontes/login', { withCredentials: true }).subscribe({
      next: (servidor) => {
        const servidorProcesados: Usuario[] = servidor.map(u => ({
          username: u.nombreUsuario || u.nombre,
          nombre: u.nombreReal || u.nombreUsuario,
          rol: u.rol === 1 ? 'AGENTE' : (u.rol === 2 ? 'OPERADOR' : 'ADMIN'),
          estado: u.estado === 1 ? 'ACTIVO' : 'INACTIVO',
          origen: 'DB'
        }));


        this.usuarios = [...servidorProcesados, ...locales.filter(l =>
          !servidorProcesados.some(s => s.username === l.username)
        )];
        this.cargando = false;
      },
      error: (err) => {
        console.error('Error al conectar con el servidor:', err);
        this.usuarios = locales;
        this.cargando = false;
      }
    });
  }

  alternarEstado(user: Usuario): void {
    const esBaja = user.estado === 'ACTIVO';
    const nuevoEstadoStr = esBaja ? 'INACTIVO' : 'ACTIVO';
    const nuevoEstadoNum = esBaja ? 0 : 1;

    const mensaje = `¿Desea ${esBaja ? 'dar de baja' : 'reactivar'} al usuario ${user.username}?`;
    if (!confirm(mensaje)) return;

    if (user.origen === 'DB') {

      this.http.put('http://localhost:8080/Horizontes/login', {
        nombreUsuario: user.username,
        estado: nuevoEstadoNum
      }, { withCredentials: true }).subscribe({
        next: () => {
          this.cargarUsuarios();
        },
        error: () => alert('Error al actualizar el estado en el servidor')
      });
    } else {

      let locales = JSON.parse(localStorage.getItem('usuarios_sistema') || '[]');
      locales = locales.map((u: any) =>
        u.username === user.username ? { ...u, estado: nuevoEstadoStr } : u
      );
      localStorage.setItem('usuarios_sistema', JSON.stringify(locales));
      this.cargarUsuarios(); // Refresca la vista
    }
  }
}
