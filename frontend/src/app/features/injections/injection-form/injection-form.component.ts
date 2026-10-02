import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { DropdownModule } from 'primeng/dropdown';
import { MessageModule } from 'primeng/message';

import { InjectionService } from '../services/injection.service';
import { InjectionPaymentMethod } from '../services/injection.models';
import { CustomerService } from '../../customers/services/customer.service';
import { Customer } from '../../customers/services/customer.models';

const PAYMENT_METHOD_OPTIONS: { label: string; value: InjectionPaymentMethod }[] = [
  { label: 'Efectivo', value: 'EFECTIVO' },
  { label: 'Tarjeta', value: 'TARJETA' },
  { label: 'Transferencia', value: 'TRANSFERENCIA' }
];

/**
 * Registro de un servicio de inyectologia. Decision confirmada: el
 * medicamento siempre lo trae el paciente (o se compro aparte con una
 * venta normal del POS) — este formulario nunca busca ni descuenta
 * inventario, medicationName es texto libre.
 */
@Component({
  selector: 'app-injection-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ButtonModule, CardModule, InputTextModule, InputNumberModule, DropdownModule, MessageModule],
  templateUrl: './injection-form.component.html'
})
export class InjectionFormComponent implements OnInit {

  customerQuery = '';
  customerResults: Customer[] = [];
  selectedCustomer: Customer | null = null;
  patientName = '';

  medicationName = '';
  dose = '';
  lotCode = '';
  applicationSite = '';
  relatedSaleId: number | null = null;
  observations = '';
  fee: number | null = null;
  paymentMethod: InjectionPaymentMethod = 'EFECTIVO';
  paymentMethodOptions = PAYMENT_METHOD_OPTIONS;

  saving = false;
  formError = '';

  private customerSearchTimeout?: ReturnType<typeof setTimeout>;

  constructor(
    private readonly injectionService: InjectionService,
    private readonly customerService: CustomerService,
    private readonly router: Router
  ) {
  }

  ngOnInit(): void {
  }

  onCustomerQueryChange(): void {
    clearTimeout(this.customerSearchTimeout);
    if (!this.customerQuery || this.customerQuery.trim().length < 2) {
      this.customerResults = [];
      return;
    }
    this.customerSearchTimeout = setTimeout(() => {
      this.customerService.search({ search: this.customerQuery, active: true, page: 0, size: 8 })
        .subscribe((result) => (this.customerResults = result.content));
    }, 300);
  }

  selectCustomer(customer: Customer): void {
    this.selectedCustomer = customer;
    this.customerQuery = '';
    this.customerResults = [];
  }

  clearCustomer(): void {
    this.selectedCustomer = null;
  }

  get canSubmit(): boolean {
    const hasPatient = !!this.selectedCustomer || this.patientName.trim().length > 0;
    return hasPatient && this.medicationName.trim().length > 0 && this.fee !== null && this.fee >= 0;
  }

  submit(): void {
    if (!this.canSubmit) {
      return;
    }
    this.saving = true;
    this.formError = '';

    this.injectionService.create({
      customerId: this.selectedCustomer?.id ?? null,
      patientName: this.selectedCustomer ? null : this.patientName.trim(),
      medicationName: this.medicationName.trim(),
      dose: this.dose || null,
      lotCode: this.lotCode || null,
      applicationSite: this.applicationSite || null,
      relatedSaleId: this.relatedSaleId,
      observations: this.observations || null,
      fee: this.fee as number,
      paymentMethod: this.paymentMethod
    }).subscribe({
      next: (record) => {
        this.saving = false;
        this.router.navigateByUrl(`/inyectologia/${record.id}`);
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.formError = err.error?.message || 'No se pudo registrar el servicio. Intenta nuevamente.';
      }
    });
  }

  goBack(): void {
    this.router.navigateByUrl('/inyectologia');
  }
}
