import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReportesComponent } from './reportes';
import { provideHttpClient } from '@angular/common/http';
import { ClienteService } from '../../../core/services/cliente.service';
import { CommonModule } from '@angular/common';

describe('ReportesComponent', () => {
  let component: ReportesComponent;
  let fixture: ComponentFixture<ReportesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({

      imports: [
        ReportesComponent,
        CommonModule
      ],

      providers: [
        provideHttpClient(),
        ClienteService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReportesComponent);
    component = fixture.componentInstance;


    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
