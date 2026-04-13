import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Destino, Paquete, Proveedor } from '../../modules/operaciones/destinos/destinos';

@Injectable({
  providedIn: 'root'
})
export class OperacionesService {
  addDestino(arg0: { nombre: string; pais: string; descripcion: string; }) {
    throw new Error('Method not implemented.');
  }
  addProveedor(arg0: { nombre: string; tipo: string; pais: string; }) {
    throw new Error('Method not implemented.');
  }
  addPaquete(arg0: { nombre: string; destino: string; duracion: string; precio: string; capacidad: string; }) {
    throw new Error('Method not implemented.');
  }
  asignarServicio(arg0: string, arg1: { proveedor: string; desc: string; costo: string; }) {
    throw new Error('Method not implemented.');
  }
  addReservacion(arg0: { paquete: string; agente: string; fecha: string; pasajeros: string[]; }) {
    throw new Error('Method not implemented.');
  }
  addPago(arg0: { reserva: string; monto: string; metodo: string; fecha: string; }) {
    throw new Error('Method not implemented.');
  }

  // Estados iniciales  BD
  private destinosSubject = new BehaviorSubject<Destino[]>([]);
  private paquetesSubject = new BehaviorSubject<Paquete[]>([]);
  private proveedoresSubject = new BehaviorSubject<Proveedor[]>([]);


  destinos$ = this.destinosSubject.asObservable();
  paquetes$ = this.paquetesSubject.asObservable();
  proveedores$ = this.proveedoresSubject.asObservable();

  constructor() {}




  getPaquetesDisponibles(): Paquete[] {
    return this.paquetesSubject.value.filter(p => p.estado && (p.capacidad > 0));
  }

  // Para Administración: Reporte de utilidades
  getReporteFinanciero() {
    const paquetes = this.paquetesSubject.value;
    return {
      totalGananciaBruta: paquetes.reduce((acc, p) => acc + (p.gananciaBruta || 0), 0),
      totalPaquetes: paquetes.length,
      destinosPopulares: this.destinosSubject.value.length
    };
  }


  actualizarDisponibilidad(nombrePaquete: string, cantidad: number) {
    const actual = this.paquetesSubject.value;
    const index = actual.findIndex(p => p.nombre === nombrePaquete);
    if (index !== -1) {
      actual[index].capacidad -= cantidad;
      this.paquetesSubject.next([...actual]);
    }
  }


  setProveedores(proveedores: any[]) {
    this.proveedoresSubject.next(proveedores);
  }

  setDestinos(nuevos: Destino[]) {
    this.destinosSubject.next([...this.destinosSubject.value, ...nuevos]);
  }

  setPaquetes(nuevos: Paquete[]) {
    this.paquetesSubject.next([...this.paquetesSubject.value, ...nuevos]);
  }
}
