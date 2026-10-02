import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { TokenStorageService, StoredSession } from './token-storage.service';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  username: string;
  fullName: string;
  roles: string[];
  permissions: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly router = inject(Router);

  private readonly sessionSignal = signal<StoredSession | null>(this.tokenStorage.read());

  readonly session = this.sessionSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.sessionSignal() !== null);

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, request).pipe(
      tap((response) => {
        const session: StoredSession = {
          accessToken: response.accessToken,
          username: response.username,
          fullName: response.fullName,
          roles: response.roles,
          permissions: response.permissions
        };
        this.tokenStorage.save(session);
        this.sessionSignal.set(session);
      })
    );
  }

  logout(): void {
    this.tokenStorage.clear();
    this.sessionSignal.set(null);
    this.router.navigateByUrl('/login');
  }

  getToken(): string | null {
    return this.sessionSignal()?.accessToken ?? null;
  }

  /** El backend es quien realmente aplica los permisos; esto solo oculta/muestra UI. */
  hasPermission(code: string): boolean {
    return this.sessionSignal()?.permissions.includes(code) ?? false;
  }

  hasRole(code: string): boolean {
    return this.sessionSignal()?.roles.includes(code) ?? false;
  }
}
