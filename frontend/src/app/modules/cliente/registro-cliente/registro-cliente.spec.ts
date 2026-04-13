import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RegistroClienteComponent } from './registro-cliente'; // Asegúrate que el nombre de la clase sea este
import { FormsModule } from '@angular/forms'; // Importante si usas ngModel
import { CommonModule } from '@angular/common';

describe('RegistroClienteComponent', () => {
  let component: RegistroClienteComponent;
  let fixture: ComponentFixture<RegistroClienteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({

      imports: [
        RegistroClienteComponent,
        CommonModule,
        FormsModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegistroClienteComponent);
    component = fixture.componentInstance;


    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
