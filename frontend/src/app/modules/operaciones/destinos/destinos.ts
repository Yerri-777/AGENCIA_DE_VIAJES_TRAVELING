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
  // --- Control de Navegacion y Filtros ---
  vistaActual: string = 'Destinos';
  filtroTexto: string = '';

  // --- Listados de Datos Centralizados ---
  destinos: Destino[] = [];
  proveedores: Proveedor[] = [];
  paquetes: Paquete[] = [];

  // --- Modelos para Creacion de Objetos ---
  nuevoDestino: Destino = { nombre: '', pais: '', descripcion: '', clima: '', imagenUrl: '' };
  nuevoProveedor: Proveedor = { nombre: '', tipo: 1, pais: '' };
  nuevoPaquete: Paquete = this.limpiarPaquete();

  // Objeto temporal para capturar servicios antes de agregarlos al paquete
  tempServicio: ServicioIncluido = { proveedor: '', descripcion: '', costo: 0 };

  constructor(
    private operacionesService: OperacionesService,
    private router: Router // Inyectado para navegación
  ) {}

  ngOnInit(): void {
    this.operacionesService.destinos$.subscribe(d => this.destinos = d);
    this.operacionesService.paquetes$.subscribe(p => this.paquetes = p);
    this.operacionesService.proveedores$.subscribe(pr => this.proveedores = pr);
  }

  // MÉTODO PARA VOLVER AL LOGIN (Asegúrate que se llame igual en el HTML)
  salirAlMenu() {
    if (confirm('¿Cerrar sesión y volver al login?')) {
      this.router.navigate(['/login']);
    }
  }

  // --- Consultas y Calculos de Interfaz ---
  get paquetesFiltrados() {
    const busqueda = this.filtroTexto.toLowerCase();
    return this.paquetes.filter(p =>
      p.nombre.toLowerCase().includes(busqueda) ||
      p.destino.toLowerCase().includes(busqueda)
    );
  }

  getResumen() {
    return {
      activos: this.paquetes.filter(p => p.estado).length,
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
  }

  trackByNombre(index: number, item: any): string {
    return item.nombre;
  }

  // --- Logica de Accion: Paquetes ---

  agregarServicio() {
    if (this.tempServicio.proveedor && this.tempServicio.costo > 0) {
      const servicioAGuardar: ServicioIncluido = {
        proveedor: this.tempServicio.proveedor,
        descripcion: this.tempServicio.descripcion || 'Servicio General',
        costo: Number(this.tempServicio.costo)
      };

      this.nuevoPaquete.servicios = [...this.nuevoPaquete.servicios, servicioAGuardar];
      this.tempServicio = { proveedor: '', descripcion: '', costo: 0 };
    } else {
      alert("Debe seleccionar un proveedor e indicar un costo valido para el servicio.");
    }
  }

  guardarPaquete() {
    if (!this.nuevoPaquete.nombre || this.nuevoPaquete.servicios.length === 0) {
      alert("El paquete requiere un nombre y al menos un servicio incluido.");
      return;
    }

    const costoTotal = this.nuevoPaquete.servicios.reduce((acc, s) => acc + s.costo, 0);
    this.nuevoPaquete.costoTotalAgencia = costoTotal;
    this.nuevoPaquete.gananciaBruta = this.nuevoPaquete.precioVenta - costoTotal;

    this.operacionesService.setPaquetes([...this.paquetes, { ...this.nuevoPaquete }]);

    alert("Paquete guardado y publicado en el sistema de ventas.");
    this.nuevoPaquete = this.limpiarPaquete();
  }

  toggleEstadoPaquete(p: Paquete) {
    p.estado = !p.estado;
    this.operacionesService.setPaquetes([...this.paquetes]);
  }

  // --- Logica de Accion: Proveedores y Destinos ---

  guardarProveedor() {
    if (this.nuevoProveedor.nombre && this.nuevoProveedor.pais) {
      const p = { ...this.nuevoProveedor, tipo: Number(this.nuevoProveedor.tipo) };
      this.operacionesService.setProveedores([...this.proveedores, p]);
      this.nuevoProveedor = { nombre: '', tipo: 1, pais: '' };
      alert("Proveedor registrado exitosamente.");
    } else {
      alert("Por favor complete los campos de nombre y pais.");
    }
  }

  guardarDestino() {
    if (this.nuevoDestino.nombre && this.nuevoDestino.pais) {
      this.operacionesService.setDestinos([...this.destinos, { ...this.nuevoDestino }]);
      this.nuevoDestino = { nombre: '', pais: '', descripcion: '', clima: '', imagenUrl: '' };
      alert("Nuevo destino agregado al catalogo.");
    } else {
      alert("El nombre y el pais del destino son campos obligatorios.");
    }
  }
}
