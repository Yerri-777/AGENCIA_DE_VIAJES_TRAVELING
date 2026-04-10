import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // 1. Verificar si está logueado
  if (!authService.estaLogueado()) {
    console.warn('[ROLE-GUARD] Usuario no logueado, redirigiendo...');
    router.navigate(['/login']);
    return false;
  }

  // 2. Obtener roles permitidos
  // Usamos un arreglo vacío por defecto si no viene nada
  const requiredRoles = (route.data?.['roles'] as number[]) || [];

  if (requiredRoles.length === 0) {
    return true;
  }

  const userRol = authService.obtenerRol;

  console.log('[ROLE-GUARD] Validando acceso:', {
    url: state.url,
    requeridos: requiredRoles,
    usuarioTiene: userRol
  });

  // 3. El administrador (3) siempre entra
  if (userRol === 3) {
    console.log('[ROLE-GUARD] Admin detectado, acceso libre.');
    return true;
  }

  // 4. Verificación estricta para Rol 1 y otros
  // Usamos Number() para asegurar que la comparación no falle
  if (requiredRoles.map(r => Number(r)).includes(Number(userRol))) {
    console.log('[ROLE-GUARD] Rol autorizado, bienvenido.');
    return true;
  }

  console.error('[ROLE-GUARD] Acceso denegado. Redirigiendo a /forbidden');
  router.navigate(['/forbidden']);
  return false;
};
