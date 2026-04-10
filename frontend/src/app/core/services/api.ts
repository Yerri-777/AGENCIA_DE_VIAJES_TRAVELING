import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {


  private readonly BASE_URL = (environment.apiBaseUrl || 'http://localhost:8080/Horizontes').replace(/\/$/, '');

  constructor(private http: HttpClient) { }


  private getOptions(params?: HttpParams) {
    return {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      }),

      params: params,
      withCredentials: true
    };
  }


  get<T>(endpoint: string, queryParams?: any): Observable<T> {
    let params = new HttpParams();
    if (queryParams) {
      for (const key in queryParams) {
        params = params.append(key, queryParams[key]);
      }
    }
    return this.http.get<T>(`${this.BASE_URL}/${endpoint}`, this.getOptions(params));
  }


  post<T>(endpoint: string, body: any): Observable<T> {
    return this.http.post<T>(`${this.BASE_URL}/${endpoint}`, body, this.getOptions());
  }


  uploadFile(endpoint: string, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('archivo', file);


    return this.http.post(`${this.BASE_URL}/${endpoint}`, formData, {
      withCredentials: true
    });
  }
}
