import { Routes } from '@angular/router';
import { LoginComponent } from './modules/auth/login/login';
import { WelcomeComponent } from './modules/shared/welcome/welcome'; // Asegúrate de que la ruta sea correcta
import { authGuard } from './core/guards/auth-guard';
import { roleGuard } from './core/guards/role-guard';

export const routes: Routes = [

  { path: '', component: WelcomeComponent },


  { path: 'login', component: LoginComponent },


  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: [3] },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
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
      },
      {
        path: 'usuarios',
        loadComponent: () => import('./modules/Usuarios/usuario').then(m => m.UsuariosComponent)
      }
    ]
  },


  {
    path: 'atencion',
    canActivate: [authGuard, roleGuard],
    data: { roles: [1] },
    children: [
      { path: '', redirectTo: 'registro-cliente', pathMatch: 'full' },
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


  {
    path: 'operaciones',
    canActivate: [authGuard, roleGuard],
    data: { roles: [2] },
    children: [
      { path: '', redirectTo: 'destinos', pathMatch: 'full' },
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


  {
    path: 'forbidden',
    loadComponent: () => import('./modules/shared/forbidden/forbidden').then(m => m.ForbiddenComponent)
  },


  {
    path: '**',
    redirectTo: ''
  }
];
