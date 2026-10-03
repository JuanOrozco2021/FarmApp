import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';

import { SettingsService } from '../../core/services/settings.service';
import { AuthService } from '../../core/services/auth.service';

/**
 * Configuracion del negocio: nombre, NIT y direccion (encabezado del
 * ticket de venta) y dias de alerta de vencimiento. GET /api/settings es
 * publico para cualquier usuario autenticado (el ticket lo necesita sin
 * importar quien cobra); solo SETTINGS_MANAGE puede guardar cambios --
 * el backend lo vuelve a exigir de todas formas, este formulario solo
 * evita que alguien sin el permiso intente algo que el servidor rechaza.
 */
@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, CardModule, InputTextModule, InputNumberModule,
    ButtonModule, MessageModule
  ],
  templateUrl: './settings.component.html'
})
export class SettingsComponent implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly settingsService = inject(SettingsService);
  protected readonly authService = inject(AuthService);

  readonly form = this.fb.nonNullable.group({
    pharmacyName: ['', Validators.required],
    nit: [''],
    address: [''],
    expirationAlertDays: this.fb.nonNullable.control<number>(30, [Validators.required, Validators.min(1)]),
    ticketPrinterName: ['']
  });

  loading = signal(true);
  saving = signal(false);
  errorMessage = signal<string | null>(null);
  savedMessage = signal<string | null>(null);

  get canManage(): boolean {
    return this.authService.hasPermission('SETTINGS_MANAGE');
  }

  ngOnInit(): void {
    this.settingsService.get().subscribe({
      next: (settings) => {
        this.form.setValue({
          pharmacyName: settings.pharmacyName,
          nit: settings.nit || '',
          address: settings.address || '',
          expirationAlertDays: settings.expirationAlertDays,
          ticketPrinterName: settings.ticketPrinterName || ''
        });
        this.loading.set(false);
        if (!this.canManage) {
          this.form.disable();
        }
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('No se pudo cargar la configuracion del negocio.');
      }
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorMessage.set(null);
    this.savedMessage.set(null);
    this.saving.set(true);

    const raw = this.form.getRawValue();
    this.settingsService.update({
      pharmacyName: raw.pharmacyName,
      nit: raw.nit || null,
      address: raw.address || null,
      expirationAlertDays: raw.expirationAlertDays,
      ticketPrinterName: raw.ticketPrinterName || null
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.savedMessage.set('Configuracion guardada.');
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo guardar la configuracion.');
      }
    });
  }
}
