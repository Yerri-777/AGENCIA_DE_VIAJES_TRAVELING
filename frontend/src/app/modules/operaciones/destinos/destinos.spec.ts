import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DestinosComponent } from './destinos'; // Nombre corregido
import { FormsModule } from '@angular/forms'; // Necesario para ngModel
import { CommonModule } from '@angular/common';

describe('DestinosComponent', () => {
  let component: DestinosComponent;
  let fixture: ComponentFixture<DestinosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      // Importamos el componente y los módulos que usa
      imports: [
        DestinosComponent,
        FormsModule,
        CommonModule
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DestinosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges(); // Ejecuta el ciclo de vida inicial
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('debería iniciar en la vista de Destinos', () => {
    expect(component.vistaActual).toBe('Destinos');
  });

  it('debería agregar un nuevo destino a la lista', () => {
    const inicial = component.destinos.length;
    component.nuevoDestino = {
      nombre: 'Lago de Atitlán',
      pais: 'Guatemala',
      descripcion: 'El lago más hermoso del mundo'
    };
    component.guardarDestino();
    expect(component.destinos.length).toBe(inicial + 1);
  });
});
