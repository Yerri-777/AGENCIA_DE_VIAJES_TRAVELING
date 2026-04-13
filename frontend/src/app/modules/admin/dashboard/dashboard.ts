import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { OperacionesService } from '../../../core/services/operaciones.service';
import { ClienteService } from '../../../core/services/cliente.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
})
export class DashboardComponent implements OnInit {

  servidorOnline: boolean = true;
  ultimaCarga: string = 'Sin registros';


  totalPaquetes: number = 0;
  totalClientes: number = 0;
  totalDestinos: number = 0;
  totalUsuarios: number = 0;

  constructor(
    private http: HttpClient,
    private router: Router,
    private operacionesService: OperacionesService,
    private clienteService: ClienteService
  ) {}

  ngOnInit(): void {
    this.verificarEstadoServidor();
    this.cargarEstadisticas();
    this.inicializarFlujosDeDatos();
    this.actualizarConteoPersonal();

    const savedDate = localStorage.getItem('ultima_carga_masiva');
    if (savedDate) {
      this.ultimaCarga = savedDate;
    }
  }


  private actualizarConteoPersonal() {
  const usuariosLocales = JSON.parse(localStorage.getItem('usuarios_sistema') || '[]');


    this.http.get<any[]>('http://localhost:8080/Horizontes/usuarios?accion=listar').subscribe({
      next: (res) => {
        this.totalUsuarios = res.length + usuariosLocales.length;
      },
      error: () => {

        this.totalUsuarios = usuariosLocales.length;
      }
    });
  }

  private inicializarFlujosDeDatos() {
    this.operacionesService.paquetes$.subscribe(p => {
      const cache = JSON.parse(localStorage.getItem('paquetes_cache') || '[]');
      this.totalPaquetes = p.length + cache.length;
    });

    this.operacionesService.destinos$.subscribe(d => {
      const cache = JSON.parse(localStorage.getItem('destinos_cache') || '[]');
      this.totalDestinos = d.length + cache.length;
    });

    this.clienteService.listar().subscribe(c => {
      const cache = JSON.parse(localStorage.getItem('clientes_locales') || '[]');
      this.totalClientes = c.length + cache.length;
    });
  }

  verificarEstadoServidor() {
    this.http.get('http://localhost:8080/Horizontes/reportes?tipo=ping', { observe: 'response' })
      .subscribe({
        next: () => {
          this.servidorOnline = true;
        },
        error: () => {
          this.servidorOnline = false;
          console.warn('Backend fuera de línea. Cambiando a Modo Local.');
          this.recalcularSoloLocal();
        }
      });
  }

  private recalcularSoloLocal() {
    this.totalPaquetes = JSON.parse(localStorage.getItem('paquetes_cache') || '[]').length;
    this.totalDestinos = JSON.parse(localStorage.getItem('destinos_cache') || '[]').length;
    this.totalClientes = JSON.parse(localStorage.getItem('clientes_locales') || '[]').length;
    this.totalUsuarios = JSON.parse(localStorage.getItem('usuarios_cache') || '[]').length;
  }

  cargarEstadisticas() {
    const logCarga = localStorage.getItem('ultima_carga_log');
    if (logCarga) {
      try {
        const data = JSON.parse(logCarga);
        this.ultimaCarga = data.fecha || this.ultimaCarga;
      } catch (e) {
        console.error("Error al procesar historial");
      }
    }
  }

  navegarA(ruta: string) {
    this.router.navigate([ruta]);
  }

  logout() {
    if (confirm('¿Desea cerrar la sesión administrativa de Horizontes Sin Límites?')) {
      localStorage.removeItem('agencia_session');
      localStorage.removeItem('user_role');
      this.router.navigate(['/login']);
    }
  }
}
