import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CargaMasivaComponent } from './carga-masiva'; // Ajustado al nombre estándar
import { provideHttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

describe('CargaMasivaComponent', () => {
  let component: CargaMasivaComponent;
  let fixture: ComponentFixture<CargaMasivaComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({

      imports: [
        CargaMasivaComponent,
        CommonModule,
        FormsModule
      ],

      providers: [
        provideHttpClient()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CargaMasivaComponent);
    component = fixture.componentInstance;


    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
