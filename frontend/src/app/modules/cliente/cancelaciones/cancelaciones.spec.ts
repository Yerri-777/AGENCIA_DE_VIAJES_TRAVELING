import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CancelacionesComponent } from './cancelaciones';
import { provideHttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { OperacionesService } from '../../../core/services/operaciones.service';

describe('CancelacionesComponent', () => {
  let component: CancelacionesComponent;
  let fixture: ComponentFixture<CancelacionesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CancelacionesComponent,
        CommonModule,
        FormsModule
      ],
      providers: [
        provideHttpClient(),
        OperacionesService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CancelacionesComponent);
    component = fixture.componentInstance;

    // Inicialización del componente
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
