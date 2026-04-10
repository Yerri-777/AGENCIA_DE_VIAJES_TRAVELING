import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
  constructor(private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Clonar request para asegurar withCredentials y añadir token si existe
    const token = localStorage.getItem('agencia_token');
    let cloned = req.clone({ withCredentials: true });
    if (token) {
      cloned = cloned.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    }

    return next.handle(cloned).pipe(
      catchError((error: HttpErrorResponse) => {

        console.error('[HTTP-ERROR-INTERCEPTOR] status=', error.status, 'url=', error.url, 'message=', error.message);
        if (error.status === 403) {

          try {
            this.router.navigate(['/forbidden']);
          } catch (e) {
            console.error('[HTTP-ERROR-INTERCEPTOR] navegacion a /forbidden fallida', e);
          }
        }

        return throwError(() => error);
      })
    );
  }
}
