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


    const usuariosLocales = JSON.parse(localStorage.getItem('usuarios_sistema') || '[]');
    const usuarioEncontrado = usuariosLocales.find((u: any) =>
      u.username === this.nombre && u.password === this.password
    );

    if (usuarioEncontrado) {
      console.log('Login exitoso (Carga Masiva):', usuarioEncontrado.username);
      this.ejecutarRedireccion(Number(usuarioEncontrado.rol));
      return;
    }


    console.log('Buscando usuario en servidor...');
    this.authService.login(this.nombre, this.password).subscribe({
      next: (user) => {
        this.isLoading = false;
        this.ejecutarRedireccion(Number(user.rol));
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMsg = 'Credenciales inválidas o error de conexión.';
        console.error('Error de login:', err);
      }
    });
  }

  private ejecutarRedireccion(rol: number) {
    this.isLoading = false;
    let ruta = '';

    // Lógica de ruteo por rol
    if (rol === 3) {
      ruta = '/admin/dashboard';
    } else if (rol === 1) {
      ruta = '/atencion/registro-cliente';
    } else if (rol === 2) {
      ruta = '/operaciones/destinos';
    } else {

      ruta = '/operaciones/destinos';
    }

    console.log(`Navegando a panel de Rol ${rol}: ${ruta}`);

    this.router.navigate([ruta]).then((navigated) => {
      if (!navigated) {
        this.errorMsg = 'No tienes permiso para acceder a esta sección.';
      }
    }).catch(err => {
      this.errorMsg = 'Error en el sistema de rutas.';
    });
  }
}
