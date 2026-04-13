import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OperacionesService } from '../../../core/services/operaciones.service';
import { ClienteService } from '../../../core/services/cliente.service';

@Component({
  selector: 'app-carga-masiva',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './carga-masiva.html',
  styleUrls: ['./carga-masiva.css'],
})
export class CargaMasivaComponent {
  selectedFile: File | null = null;
  uploading = false;
  progress = 0;
  resultado: any = null;
  errorMsg: string | null = null;

  constructor(
    private operacionesService: OperacionesService,
    private clienteService: ClienteService
  ) {}

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.errorMsg = !this.selectedFile.name.endsWith('.txt') ? 'Solo archivos .txt' : null;
      this.resultado = null;
    }
  }

  upload() {
    if (!this.selectedFile) return;
    this.uploading = true;
    this.progress = 0;
    this.resultado = null;

    const reader = new FileReader();
    reader.onload = (e: any) => {
      const contenido = e.target.result;
      const lineas = contenido.split(/\r?\n/);
      this.procesarArchivoLocal(lineas);
    };
    reader.readAsText(this.selectedFile, 'UTF-8');
  }

 private procesarArchivoLocal(lineas: string[]) {
    let procesadas = 0;
    let errores: string[] = [];

    lineas.forEach((linea, index) => {
      const l = linea.trim();
      // Ignorar vacíos o comentarios, pero contar la línea para el índice de error
      if (!l || l.startsWith('#')) return;

      try {
        // Regex mejorado para capturar lo que está entre paréntesis
        const match = l.match(/^(\w+)\s*\((.*)\)$/);
        if (!match) {
          errores.push(`Línea ${index + 1}: Error de formato (faltan paréntesis).`);
          return;
        }

        const comando = match[1].toUpperCase();
        // Separador inteligente que respeta comas dentro de comillas
        const params = match[2].split(/,(?=(?:(?:[^"]*"){2})*[^"]*$)/)
                                .map(p => p.trim().replace(/^"|"$/g, ''));

        const service = this.operacionesService as any;

        switch (comando) {
          case 'USUARIO':
            this.validarYRegistrarUsuario(params, index, errores);
            break;
          case 'CLIENTE':
            // Aseguramos que CLIENTE se guarde sí o sí en local
            this.clienteService.crearLocal({
                dpi: params[0],
                nombre: params[1],
                nacimiento: params[2],
                tel: params[3],
                email: params[4],
                nacionalidad: params[5]
            });
            console.log(`Cliente ${params[1]} capturado.`);
            break;
          case 'DESTINO':
            this.ejecutarSeguro(service, 'addDestino', { nombre: params[0], pais: params[1], descripcion: params[2] }, 'destinos_cache');
            break;
          case 'PROVEEDOR':
            this.ejecutarSeguro(service, 'addProveedor', { nombre: params[0], tipo: params[1], pais: params[2] }, 'proveedores_cache');
            break;
          case 'PAQUETE':
            this.ejecutarSeguro(service, 'addPaquete', { nombre: params[0], destino: params[1], duracion: params[2], precio: params[3], capacidad: params[4] }, 'paquetes_cache');
            break;
          case 'SERVICIO_PAQUETE':
            this.ejecutarSeguro(service, 'asignarServicio', { paquete: params[0], proveedor: params[1], desc: params[2], costo: params[3] }, 'servicios_cache');
            break;
          case 'RESERVACION':
            this.ejecutarSeguro(service, 'addReservacion', { paquete: params[0], agente: params[1], fecha: params[2], pasajeros: params[3]?.split('|') }, 'reservas_cache');
            break;
          case 'PAGO':
            this.ejecutarSeguro(service, 'addPago', { reserva: params[0], monto: params[1], metodo: params[2], fecha: params[3] }, 'pagos_cache');
            break;
          default:
            errores.push(`Línea ${index + 1}: Comando ${comando} no reconocido.`);
        }
        procesadas++;
      } catch (e) {
        errores.push(`Línea ${index + 1}: Error procesando datos.`);
      }
    });

    this.finalizarCarga(procesadas, errores);
  }


  private ejecutarSeguro(service: any, metodo: string, data: any, cacheKey: string) {
    try {
      // Intentamos llamar al servicio. Si no existe el método o lanza "Not Implemented", saltará al catch.
      if (typeof service[metodo] === 'function') {
        service[metodo](data);
      } else {
        throw new Error('Fallback to local');
      }
    } catch (e) {
      // GUARDADO LOCAL: Si el servicio falla o no está listo, guardamos en LocalStorage
      const storage = JSON.parse(localStorage.getItem(cacheKey) || '[]');
      storage.push(data);
      localStorage.setItem(cacheKey, JSON.stringify(storage));
      console.log(`Guardado en local (${cacheKey}):`, data.nombre || data.paquete || 'Registro');
    }
  }

 private validarYRegistrarUsuario(p: any[], i: number, err: string[]) {
    if (p.length < 3) {
      err.push(`Línea ${i + 1}: Faltan datos de usuario.`);
      return;
    }
    const usuarios = JSON.parse(localStorage.getItem('usuarios_sistema') || '[]');

    // Aseguramos que el rol sea numérico para evitar errores de navegación
    usuarios.push({
      username: p[0],
      password: p[1],
      rol: Number(p[2])
    });

    localStorage.setItem('usuarios_sistema', JSON.stringify(usuarios));
    console.log(`Usuario ${p[0]} (Rol: ${p[2]}) registrado en caché local.`);
  }

  private finalizarCarga(procesadas: number, errores: string[]) {
    this.resultado = {
      procesadas,
      errores: errores.length,
      mensaje: errores.length === 0 ? '¡Carga Exitosa!' : 'Carga finalizada con advertencias.',
      detalles: errores
    };
    this.uploading = false;
    this.progress = 100;

    localStorage.setItem('ultima_carga_masiva', new Date().toLocaleString());
  }
}
