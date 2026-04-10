import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.estaLogueado()) {
    return true; // Adelante, pase usted
  } else {
    // No está logueado, lo mandamos al login
    router.navigate(['/login']);
    return false;
  }
};
