import { Routes } from '@angular/router';
import { LoginComponent } from './modules/auth/login/login';
import { authGuard } from './core/guards/auth-guard';
import { roleGuard } from './core/guards/role-guard';

export const routes: Routes = [
  // 1. RUTA INICIAL
  {
    path: '',
    loadComponent: () => import('./modules/shared/welcome/welcome').then(m => m.WelcomeComponent),
    pathMatch: 'full'
  },

  // 2. RUTA DE AUTENTICACIÓN
  {
    path: 'login',
    component: LoginComponent
  },

  // 3. RUTAS PROTEGIDAS: ADMINISTRADOR (Rol 3)
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: [3] }, // El Guard verificará que el usuario tenga rol 3
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./modules/admin/dashboard/dashboard').then(m => m.DashboardComponent)
      },
      {
        path: 'reportes',
        loadComponent: () => import('./modules/admin/reportes/reportes').then(m => m.ReportesComponent)
      },
      {
        path: 'carga-masiva',
        loadComponent: () => import('./modules/admin/carga-masiva/carga-masiva').then(m => m.CargaMasivaComponent)
      }
    ]
  },

  // 4. RUTAS PROTEGIDAS: ATENCIÓN AL CLIENTE (Rol 1)
  {
    path: 'atencion', // Cambiado a 'atencion' para coincidir con el nombre del área
    canActivate: [authGuard, roleGuard],
    data: { roles: [1] },
    children: [
      {
        path: '',
        redirectTo: 'registro-cliente',
        pathMatch: 'full'
      },
      {
        path: 'registro-cliente',
        loadComponent: () => import('./modules/cliente/registro-cliente/registro-cliente').then(m => m.RegistroClienteComponent)
      },
      {
        path: 'nueva-reservacion',
        loadComponent: () => import('./modules/cliente/nueva-reservacion/nueva-reservacion').then(m => m.NuevaReservacionComponent)
      },
      {
        path: 'pagos',
        loadComponent: () => import('./modules/cliente/pagos/pagos').then(m => m.PagosComponent)
      }
    ]
  },

  // 5. RUTAS PROTEGIDAS: OPERACIONES (Rol 2)
  {
    path: 'operaciones',
    canActivate: [authGuard, roleGuard],
    data: { roles: [2] },
    children: [
      {
        path: '',
        redirectTo: 'destinos',
        pathMatch: 'full'
      },
      {
        path: 'destinos',
        loadComponent: () => import('./modules/operaciones/destinos/destinos').then(m => m.DestinosComponent)
      },
      {
        path: 'paquetes',
        loadComponent: () => import('./modules/operaciones/paquetes/paquetes').then(m => m.PaquetesComponent)
      }
    ]
  },

  // 6. MANEJO DE ERRORES
  {
    path: 'forbidden',
    loadComponent: () => import('./modules/shared/forbidden/forbidden').then(m => m.ForbiddenComponent)
  },

  // COMODÍN: Redirigir cualquier otra ruta al login
  {
    path: '**',
    redirectTo: 'login'
  }
];
