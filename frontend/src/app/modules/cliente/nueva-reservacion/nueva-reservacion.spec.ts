import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NuevaReservacionComponent } from './nueva-reservacion';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { OperacionesService } from '../../../core/services/operaciones.service';
import { ClienteService } from '../../../core/services/cliente.service';

describe('NuevaReservacionComponent', () => {
  let component: NuevaReservacionComponent;
  let fixture: ComponentFixture<NuevaReservacionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({

      imports: [
        NuevaReservacionComponent,
        CommonModule,
        FormsModule
      ],

      providers: [
        provideHttpClient(),
        provideRouter([]),
        OperacionesService,
        ClienteService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NuevaReservacionComponent);
    component = fixture.componentInstance;


    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
