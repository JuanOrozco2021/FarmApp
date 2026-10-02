import { Injectable } from '@angular/core';

export interface StoredSession {
  accessToken: string;
  username: string;
  fullName: string;
  roles: string[];
  permissions: string[];
}

const STORAGE_KEY = 'boticario.session';

/**
 * Unico punto de lectura/escritura de la sesion en el navegador.
 * Si en el futuro se cambia a cookies httpOnly, solo este servicio cambia.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {

  save(session: StoredSession): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  read(): StoredSession | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredSession) : null;
  }

  clear(): void {
    localStorage.removeItem(STORAGE_KEY);
  }
}
