import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CargaService, RespuestaCarga } from '../../../core/services/carga.service';
import { OperacionesService } from '../../../core/services/operaciones.service'; // Inyectado
import { HttpEventType, HttpEvent } from '@angular/common/http';

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
  resultado: RespuestaCarga | null = null;
  errorMsg: string | null = null;

  constructor(
    private cargaService: CargaService,
    private operacionesService: OperacionesService // Agregado
  ) {}

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.errorMsg = !this.selectedFile.name.endsWith('.txt') ? 'Solo archivos .txt' : null;
    }
  }

  upload() {
    if (!this.selectedFile) return;
    this.uploading = true;

    this.cargaService.uploadFile(this.selectedFile).subscribe({
      next: (event: HttpEvent<RespuestaCarga>) => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          this.progress = Math.round((100 * event.loaded) / event.total);
        } else if (event.type === HttpEventType.Response) {
          this.resultado = event.body || null;

          // --- AQUÍ SUCEDE LA MAGIA DEL FLUJO ---
          if (this.resultado) {
            // Si el backend devuelve las listas, las inyectamos al servicio global
            if (this.resultado.destinos) this.operacionesService.setDestinos(this.resultado.destinos);
            if (this.resultado.proveedores) this.operacionesService.setProveedores(this.resultado.proveedores);
          }

          this.uploading = false;
          this.progress = 100;
          alert("¡Carga masiva procesada con éxito!");
        }
      },
      error: (err) => {
        this.uploading = false;
        this.errorMsg = 'Error al procesar el archivo.';
      }
    });
  }
}
