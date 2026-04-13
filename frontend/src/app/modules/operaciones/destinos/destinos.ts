import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OperacionesService } from '../../../core/services/operaciones.service';
import { Router } from '@angular/router';

export interface Destino {
  nombre: string;
  pais: string;
  descripcion: string;
  clima?: string;
  imagenUrl?: string;
}

export interface Proveedor {
  nombre: string;
  tipo: number;
  pais: string;
}

export interface ServicioIncluido {
  proveedor: string;
  descripcion: string;
  costo: number;
}

export interface Paquete {
  nombre: string;
  destino: string;
  duracion: number;
  precioVenta: number;
  capacidad: number;
  servicios: ServicioIncluido[];
  estado: boolean;
  costoTotalAgencia?: number;
  gananciaBruta?: number;
}

@Component({
  selector: 'app-destinos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './destinos.html',
  styleUrls: ['./destinos.css'],
})
export class DestinosComponent implements OnInit {

  vistaActual: string = 'Destinos';
  filtroTexto: string = '';
  editandoPaquete: boolean = false;
  editandoDestino: boolean = false;


  destinos: Destino[] = [];
  proveedores: Proveedor[] = [];
  paquetes: Paquete[] = [];

  nuevoDestino: Destino = { nombre: '', pais: '', descripcion: '', clima: '', imagenUrl: '' };
  nuevoProveedor: Proveedor = { nombre: '', tipo: 1, pais: '' };
  nuevoPaquete: Paquete = this.limpiarPaquete();


  tempServicio: ServicioIncluido = { proveedor: '', descripcion: '', costo: 0 };

  constructor(
    private operacionesService: OperacionesService,
    private router: Router
  ) {}

  ngOnInit(): void {

    this.operacionesService.destinos$.subscribe(d => {
      const cache = JSON.parse(localStorage.getItem('destinos_cache') || '[]');
      const unificados = [...d, ...cache];
      const mapa = new Map(unificados.map(item => [item.nombre, item]));
      this.destinos = Array.from(mapa.values());
    });

    this.operacionesService.paquetes$.subscribe(p => {
      const cache = JSON.parse(localStorage.getItem('paquetes_cache') || '[]');
      const unificados = [...p, ...cache];

      const mapa = new Map(unificados.map(item => {
        const pkgValido = {
          ...item,
          estado: true,
          precioVenta: item.precioVenta || (item as any).precio || 0
        };
        return [pkgValido.nombre, pkgValido];
      }));
      this.paquetes = Array.from(mapa.values());
    });

    this.operacionesService.proveedores$.subscribe(pr => {
      const cache = JSON.parse(localStorage.getItem('proveedores_cache') || '[]');
      const unificados = [...pr, ...cache];
      const mapa = new Map(unificados.map(item => [item.nombre, item]));
      this.proveedores = Array.from(mapa.values());
    });
  }

  salirAlMenu() {
    if (confirm('¿Cerrar sesión y volver al login?')) {
      localStorage.removeItem('agencia_session');
      localStorage.removeItem('user_role');
      this.router.navigate(['/login']);
    }
  }


  get paquetesFiltrados() {
    const busqueda = this.filtroTexto.toLowerCase();
    return this.paquetes.filter(p =>
      p.nombre.toLowerCase().includes(busqueda) ||
      p.destino.toLowerCase().includes(busqueda)
    );
  }

  getResumen() {
    return {
      activos: this.paquetes.filter(p => p.estado !== false).length,
      bajaDisponibilidad: this.paquetes.filter(p => p.capacidad < 5).length,
      totalDestinos: this.destinos.length
    };
  }

  getTipoProveedor(tipo: number): string {
    const t = Number(tipo);
    const tipos: { [key: number]: string } = {
      1: 'Aerolinea', 2: 'Hotel', 3: 'Tour Operador', 4: 'Traslado', 5: 'Otro'
    };
    return tipos[t] || 'Otro';
  }

  limpiarPaquete(): Paquete {
    this.editandoPaquete = false;
    return {
      nombre: '',
      destino: '',
      duracion: 1,
      precioVenta: 0,
      capacidad: 20,
      servicios: [],
      estado: true
    };
  }

  cambiarVista(vista: string) {
    this.vistaActual = vista;
    if (vista !== 'Paquetes') this.cancelarEdicion();
  }

  trackByNombre(index: number, item: any): string {
    return item.nombre;
  }



  prepararEdicion(p: Paquete) {

    this.nuevoPaquete = JSON.parse(JSON.stringify(p));
    this.editandoPaquete = true;
    this.vistaActual = 'Paquetes';
  }

  cancelarEdicion() {
    this.nuevoPaquete = this.limpiarPaquete();
    this.editandoPaquete = false;
  }

  agregarServicio() {
    if (this.tempServicio.proveedor && this.tempServicio.costo > 0) {
      const servicioAGuardar: ServicioIncluido = {
        proveedor: this.tempServicio.proveedor,
        descripcion: this.tempServicio.descripcion || 'Servicio General',
        costo: Number(this.tempServicio.costo)
      };

      this.nuevoPaquete.servicios.push(servicioAGuardar);
      this.tempServicio = { proveedor: '', descripcion: '', costo: 0 };
    } else {
      alert("Debe seleccionar un proveedor e indicar un costo válido.");
    }
  }

  removeServicio(index: number) {
    this.nuevoPaquete.servicios.splice(index, 1);
  }

  guardarPaquete() {
    if (!this.nuevoPaquete.nombre || this.nuevoPaquete.servicios.length === 0) {
      alert("El paquete requiere un nombre y al menos un servicio incluido.");
      return;
    }

    const costoTotal = this.nuevoPaquete.servicios.reduce((acc, s) => acc + Number(s.costo), 0);
    this.nuevoPaquete.costoTotalAgencia = costoTotal;
    this.nuevoPaquete.gananciaBruta = this.nuevoPaquete.precioVenta - costoTotal;
    this.nuevoPaquete.estado = true; // Asegurar activo

    let nuevosPaquetes = [...this.paquetes];
    const idx = nuevosPaquetes.findIndex(x => x.nombre === this.nuevoPaquete.nombre);

    if (idx !== -1) {
      nuevosPaquetes[idx] = { ...this.nuevoPaquete };
    } else {
      nuevosPaquetes.push({ ...this.nuevoPaquete });
    }

    this.operacionesService.setPaquetes(nuevosPaquetes);
    localStorage.setItem('paquetes_cache', JSON.stringify(nuevosPaquetes));

    alert(this.editandoPaquete ? "Paquete actualizado correctamente." : "Paquete guardado y publicado.");
    this.nuevoPaquete = this.limpiarPaquete();
    this.vistaActual = 'Destinos';
  }

  toggleEstadoPaquete(p: Paquete) {
    p.estado = !p.estado;
    this.operacionesService.setPaquetes([...this.paquetes]);
    localStorage.setItem('paquetes_cache', JSON.stringify(this.paquetes));
  }


  prepararEdicionDestino(d: Destino) {
    this.nuevoDestino = { ...d };
    this.editandoDestino = true;
    this.vistaActual = 'Destinos';
  }

  guardarProveedor() {
    if (this.nuevoProveedor.nombre && this.nuevoProveedor.pais) {
      const p = { ...this.nuevoProveedor, tipo: Number(this.nuevoProveedor.tipo) };
      const listaActualizada = [...this.proveedores, p];
      this.operacionesService.setProveedores(listaActualizada);
      localStorage.setItem('proveedores_cache', JSON.stringify(listaActualizada));

      this.nuevoProveedor = { nombre: '', tipo: 1, pais: '' };
      alert("Proveedor registrado exitosamente.");
    } else {
      alert("Por favor complete los campos de nombre y pais.");
    }
  }

  guardarDestino() {
    if (this.nuevoDestino.nombre && this.nuevoDestino.pais) {
      let listaActualizada = [...this.destinos];
      const idx = listaActualizada.findIndex(x => x.nombre === this.nuevoDestino.nombre);

      if (idx !== -1) {
        listaActualizada[idx] = { ...this.nuevoDestino };
      } else {
        listaActualizada.push({ ...this.nuevoDestino });
      }

      this.operacionesService.setDestinos(listaActualizada);
      localStorage.setItem('destinos_cache', JSON.stringify(listaActualizada));

      alert(this.editandoDestino ? "Destino actualizado." : "Nuevo destino agregado.");
      this.nuevoDestino = { nombre: '', pais: '', descripcion: '', clima: '', imagenUrl: '' };
      this.editandoDestino = false;
    } else {
      alert("El nombre y el pais del destino son campos obligatorios.");
    }
  }
}
