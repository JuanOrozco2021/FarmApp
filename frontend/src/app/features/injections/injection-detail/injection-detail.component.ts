import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';

import { InjectionService } from '../services/injection.service';
import { InjectionRecordDetail, InjectionStatus } from '../services/injection.models';
import { AuthService } from '../../../core/services/auth.service';

const STATUS_SEVERITY: Record<InjectionStatus, 'success' | 'danger'> = {
  ACTIVA: 'success',
  ANULADA: 'danger'
};

@Component({
  selector: 'app-injection-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, CardModule, TagModule, ButtonModule, DialogModule, InputTextModule, MessageModule],
  templateUrl: './injection-detail.component.html'
})
export class InjectionDetailComponent implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly injectionService = inject(InjectionService);
  protected readonly authService = inject(AuthService);

  record = signal<InjectionRecordDetail | null>(null);
  processing = signal(false);
  cancelError = '';

  showCancelDialog = false;
  cancelForm = this.fb.nonNullable.group({
    reason: ['', Validators.required]
  });

  private recordId!: number;

  ngOnInit(): void {
    this.recordId = Number(this.route.snapshot.paramMap.get('id'));
    this.reload();
  }

  reload(): void {
    this.injectionService.findById(this.recordId).subscribe((record) => this.record.set(record));
  }

  statusSeverity(status: InjectionStatus): 'success' | 'danger' {
    return STATUS_SEVERITY[status];
  }

  goToCustomer(customerId: number): void {
    this.router.navigateByUrl(`/clientes/${customerId}`);
  }

  goToRelatedSale(saleId: number): void {
    this.router.navigateByUrl(`/ventas/${saleId}`);
  }

  openCancelDialog(): void {
    this.cancelError = '';
    this.cancelForm.reset({ reason: '' });
    this.showCancelDialog = true;
  }

  submitCancel(): void {
    if (this.cancelForm.invalid) {
      this.cancelForm.markAllAsTouched();
      return;
    }
    this.processing.set(true);
    this.cancelError = '';

    this.injectionService.cancel(this.recordId, this.cancelForm.getRawValue().reason).subscribe({
      next: (record) => {
        this.record.set(record);
        this.processing.set(false);
        this.showCancelDialog = false;
      },
      error: (err) => {
        this.processing.set(false);
        this.cancelError = err.error?.message || 'No se pudo anular el registro.';
      }
    });
  }

  goBack(): void {
    this.router.navigateByUrl('/inyectologia');
  }
}
