import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { PasswordModule } from 'primeng/password';
import { MessageModule } from 'primeng/message';

import { AuthService } from '../../core/services/auth.service';
import { AccountService } from '../../core/services/account.service';

/** La confirmacion debe coincidir con la nueva contrasena. */
function passwordsMatch(): ValidatorFn {
  return (group) => {
    const newPassword = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return newPassword && confirmPassword && newPassword !== confirmPassword ? { mismatch: true } : null;
  };
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ButtonModule, DialogModule, ReactiveFormsModule, PasswordModule, MessageModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss'
})
export class ShellComponent {

  private readonly fb = inject(FormBuilder);
  private readonly accountService = inject(AccountService);

  showPasswordDialog = false;
  saving = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  }, { validators: passwordsMatch() });

  constructor(protected readonly authService: AuthService) {
  }

  logout(): void {
    this.authService.logout();
  }

  openPasswordDialog(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.passwordForm.reset({ currentPassword: '', newPassword: '', confirmPassword: '' });
    this.showPasswordDialog = true;
  }

  submitPasswordChange(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.saving.set(true);
    const raw = this.passwordForm.getRawValue();

    this.accountService.changePassword({
      currentPassword: raw.currentPassword,
      newPassword: raw.newPassword
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.successMessage.set('Contrasena actualizada correctamente.');
        this.passwordForm.reset({ currentPassword: '', newPassword: '', confirmPassword: '' });
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo cambiar la contrasena.');
      }
    });
  }
}
