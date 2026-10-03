import { Injectable } from '@angular/core';

declare const qz: any;

/**
 * Puente hacia QZ Tray (programa local que debe estar instalado y
 * corriendo en el equipo donde esta conectada la impresora POS). Permite
 * enviar comandos ESC/POS en crudo directo a la impresora, evitando el
 * dialogo de impresion del navegador.
 */
@Injectable({ providedIn: 'root' })
export class QzPrintService {

  private conectando: Promise<void> | null = null;

  private async asegurarConexion(): Promise<void> {
    if (typeof qz === 'undefined') {
      throw new Error(
        'No se encontro la libreria qz-tray en el navegador. Verifica angular.json ' +
        'y que QZ Tray este instalado y corriendo en este equipo.'
      );
    }
    if (qz.websocket.isActive()) {
      return;
    }
    if (!this.conectando) {
      // Sin certificado pago: la primera vez QZ Tray muestra un aviso de
      // seguridad que el usuario debe aceptar (opcion gratuita/sin firma).
      qz.security.setCertificatePromise((resolve: (v?: any) => void) => resolve());
      qz.security.setSignaturePromise(() => (resolve: (v?: any) => void) => resolve());
      this.conectando = qz.websocket.connect().finally(() => { this.conectando = null; });
    }
    await this.conectando;
  }

  async imprimirComandos(nombreImpresora: string, comandos: string[]): Promise<void> {
    await this.asegurarConexion();
    const config = qz.configs.create(nombreImpresora);
    await qz.print(config, comandos);
  }

  async listarImpresoras(): Promise<string[]> {
    await this.asegurarConexion();
    return await qz.printers.find();
  }
}
