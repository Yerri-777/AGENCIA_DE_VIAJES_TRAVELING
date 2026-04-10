import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent {
  nombre: string = '';
  password: string = '';
  errorMsg: string = '';
  isLoading: boolean = false;

  constructor(private authService: AuthService, private router: Router) {}

  onLogin() {
    if (!this.nombre || !this.password) {
      this.errorMsg = 'Por favor, completa todos los campos.';
      return;
    }

    this.isLoading = true;
    this.errorMsg = '';

    console.log('LoginComponent: Iniciando sesión para', this.nombre);

    this.authService.login(this.nombre, this.password).subscribe({
      next: (user) => {
        // 1. Apagamos el estado de carga inmediatamente al recibir respuesta
        this.isLoading = false;

        // 2. Forzamos que el rol sea un número para evitar fallos en la comparación
        const userRol = Number(user.rol);
        console.log('Login exitoso, procesando navegación para rol:', userRol);

        let ruta = '';

        // 3. Lógica de ruteo mejorada
        if (userRol === 3) {
          ruta = '/admin/dashboard';
        } else if (userRol === 1) {
          ruta = '/atencion/registro-cliente';
        } else if (userRol === 2) {
          ruta = '/operaciones/destinos';
        } else {
          // Si el rol es extraño, mandamos a una ruta segura por defecto
          ruta = '/operaciones/destinos';
        }

        console.log('Intentando navegar a:', ruta);

        // 4. Ejecución de la navegación con manejo de éxito/error
        this.router.navigate([ruta]).then((navigated) => {
          if (navigated) {
            console.log('Navegación completada con éxito a:', ruta);
          } else {
            console.error('La navegación fue rechazada. Revisa los Guards en app.routes.ts');
            this.errorMsg = 'No tienes permiso para acceder a esa sección.';
          }
        }).catch(err => {
          console.error('Error al navegar:', err);
          this.errorMsg = 'Error en la ruta del sistema.';
        });
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMsg = 'Credenciales inválidas o error de conexión con el servidor.';
        console.error('Error de login capturado:', err);
      }
    });
  }
}
