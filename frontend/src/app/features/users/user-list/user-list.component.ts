import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { MultiSelectModule } from 'primeng/multiselect';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { TooltipModule } from 'primeng/tooltip';

import { UserService } from '../services/user.service';
import { RoleOption, UserSummary } from '../services/user.models';
import { AuthService } from '../../../core/services/auth.service';

/** Al menos un rol asignado (Validators.required no sirve para arrays: [] no es null). */
function atLeastOneRole(control: AbstractControl): ValidationErrors | null {
  const value = control.value as string[] | null;
  return value && value.length > 0 ? null : { required: true };
}

/**
 * Administracion de usuarios del sistema: crear cuentas de personal,
 * activar/desactivar, reasignar roles y resetear contrasenas. Toda la
 * pantalla requiere USER_MANAGE -- el backend lo exige en cada endpoint,
 * este componente solo evita mostrarla a quien no lo tiene.
 */
@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, TableModule, ButtonModule, InputTextModule,
    TagModule, DialogModule, MultiSelectModule, MessageModule, PasswordModule, TooltipModule
  ],
  templateUrl: './user-list.component.html'
})
export class UserListComponent implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  protected readonly authService = inject(AuthService);

  users: UserSummary[] = [];
  roles: RoleOption[] = [];
  loading = signal(true);
  errorMessage = signal<string | null>(null);

  get canManage(): boolean {
    return this.authService.hasPermission('USER_MANAGE');
  }

  // --- Crear usuario ---
  showCreateDialog = false;
  savingCreate = signal(false);
  createError = signal<string | null>(null);
  createForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(60)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    fullName: ['', Validators.required],
    roleCodes: this.fb.nonNullable.control<string[]>([], atLeastOneRole)
  });

  // --- Editar roles ---
  showRolesDialog = false;
  savingRoles = signal(false);
  rolesError = signal<string | null>(null);
  rolesTargetUser: UserSummary | null = null;
  rolesForm = this.fb.nonNullable.group({
    roleCodes: this.fb.nonNullable.control<string[]>([], atLeastOneRole)
  });

  // --- Resetear contrasena ---
  showResetDialog = false;
  savingReset = signal(false);
  resetError = signal<string | null>(null);
  resetTargetUser: UserSummary | null = null;
  resetForm = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]]
  });

  ngOnInit(): void {
    if (!this.canManage) {
      this.loading.set(false);
      return;
    }
    this.load();
    this.userService.findRoles().subscribe({
      next: (roles) => (this.roles = roles),
      error: () => this.errorMessage.set('No se pudieron cargar los roles disponibles.')
    });
  }

  load(): void {
    this.loading.set(true);
    this.userService.findAll().subscribe({
      next: (users) => {
        this.users = users;
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo cargar la lista de usuarios.');
      }
    });
  }

  roleName(code: string): string {
    return this.roles.find((r) => r.code === code)?.name ?? code;
  }

  // --- Crear usuario ---
  openCreate(): void {
    this.createError.set(null);
    this.createForm.reset({ username: '', email: '', password: '', fullName: '', roleCodes: [] });
    this.showCreateDialog = true;
  }

  submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.createError.set(null);
    this.savingCreate.set(true);
    const raw = this.createForm.getRawValue();

    this.userService.create({
      username: raw.username,
      email: raw.email,
      password: raw.password,
      fullName: raw.fullName,
      roleCodes: raw.roleCodes
    }).subscribe({
      next: () => {
        this.savingCreate.set(false);
        this.showCreateDialog = false;
        this.load();
      },
      error: (err) => {
        this.savingCreate.set(false);
        this.createError.set(err?.error?.message ?? 'No se pudo crear el usuario.');
      }
    });
  }

  // --- Activar / desactivar ---
  toggleActive(user: UserSummary): void {
    this.userService.setActive(user.id, !user.active).subscribe({
      next: () => this.load(),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'No se pudo cambiar el estado del usuario.')
    });
  }

  // --- Editar roles ---
  openRoles(user: UserSummary): void {
    this.rolesTargetUser = user;
    this.rolesError.set(null);
    this.rolesForm.reset({ roleCodes: [...user.roles] });
    this.showRolesDialog = true;
  }

  submitRoles(): void {
    if (this.rolesForm.invalid || !this.rolesTargetUser) {
      this.rolesForm.markAllAsTouched();
      return;
    }
    this.rolesError.set(null);
    this.savingRoles.set(true);
    const roleCodes = this.rolesForm.getRawValue().roleCodes;

    this.userService.updateRoles(this.rolesTargetUser.id, roleCodes).subscribe({
      next: () => {
        this.savingRoles.set(false);
        this.showRolesDialog = false;
        this.load();
      },
      error: (err) => {
        this.savingRoles.set(false);
        this.rolesError.set(err?.error?.message ?? 'No se pudieron actualizar los roles.');
      }
    });
  }

  // --- Resetear contrasena ---
  openReset(user: UserSummary): void {
    this.resetTargetUser = user;
    this.resetError.set(null);
    this.resetForm.reset({ newPassword: '' });
    this.showResetDialog = true;
  }

  submitReset(): void {
    if (this.resetForm.invalid || !this.resetTargetUser) {
      this.resetForm.markAllAsTouched();
      return;
    }
    this.resetError.set(null);
    this.savingReset.set(true);
    const newPassword = this.resetForm.getRawValue().newPassword;

    this.userService.resetPassword(this.resetTargetUser.id, newPassword).subscribe({
      next: () => {
        this.savingReset.set(false);
        this.showResetDialog = false;
      },
      error: (err) => {
        this.savingReset.set(false);
        this.resetError.set(err?.error?.message ?? 'No se pudo resetear la contrasena.');
      }
    });
  }
}
