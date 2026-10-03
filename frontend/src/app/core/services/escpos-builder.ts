const ESC = '\x1B';
const GS = '\x1D';

function quitarAcentos(texto: string): string {
  const mapa: Record<string, string> = {
    'á': 'a', 'é': 'e', 'í': 'i', 'ó': 'o', 'ú': 'u',
    'Á': 'A', 'É': 'E', 'Í': 'I', 'Ó': 'O', 'Ú': 'U',
    'ñ': 'n', 'Ñ': 'N', 'ü': 'u', 'Ü': 'U', '¿': '?', '¡': '!'
  };
  return texto.replace(/[áéíóúÁÉÍÓÚñÑüÜ¿¡]/g, (c) => mapa[c] ?? c);
}

/**
 * Arma el contenido de un ticket como comandos ESC/POS en crudo, listos
 * para enviar directo a la impresora via QZ Tray (sin pasar por el motor
 * de impresion/rasterizado del navegador, que es lo que causaba el
 * borroneo en la impresora termica de 58mm).
 */
export class EscPosBuilder {
  private comandos: string[] = [];
  private anchoColumnas: number;

  constructor(anchoColumnas: number = 32) {
    this.anchoColumnas = anchoColumnas;
    this.comandos.push(ESC + '@'); // inicializa la impresora
  }

  alinearIzquierda(): this { this.comandos.push(ESC + 'a' + '\x00'); return this; }
  alinearCentro(): this { this.comandos.push(ESC + 'a' + '\x01'); return this; }
  alinearDerecha(): this { this.comandos.push(ESC + 'a' + '\x02'); return this; }

  negritaOn(): this { this.comandos.push(ESC + 'E' + '\x01'); return this; }
  negritaOff(): this { this.comandos.push(ESC + 'E' + '\x00'); return this; }

  dobleAlturaOn(): this { this.comandos.push(GS + '!' + '\x01'); return this; }
  tamanoNormal(): this { this.comandos.push(GS + '!' + '\x00'); return this; }

  texto(linea: string): this {
    this.comandos.push(quitarAcentos(linea) + '\n');
    return this;
  }

  lineaSeparadora(caracter: string = '-'): this {
    this.comandos.push(caracter.repeat(this.anchoColumnas) + '\n');
    return this;
  }

  /** Dos columnas en una sola linea, ej: "Paracetamol x2"   "10,000" */
  filaDosColumnas(izquierda: string, derecha: string): this {
    const izq = quitarAcentos(izquierda);
    const der = quitarAcentos(derecha);
    if (izq.length + der.length >= this.anchoColumnas) {
      const izqCorta = izq.substring(0, Math.max(0, this.anchoColumnas - der.length - 1));
      const espacios = Math.max(1, this.anchoColumnas - izqCorta.length - der.length);
      this.comandos.push(izqCorta + ' '.repeat(espacios) + der + '\n');
    } else {
      const espacios = this.anchoColumnas - izq.length - der.length;
      this.comandos.push(izq + ' '.repeat(espacios) + der + '\n');
    }
    return this;
  }

  saltosLinea(n: number = 1): this {
    this.comandos.push('\n'.repeat(n));
    return this;
  }

  cortarPapel(): this {
    this.comandos.push('\n\n\n');
    this.comandos.push(GS + 'V' + '\x42' + '\x00'); // corte parcial
    return this;
  }

  build(): string[] {
    return this.comandos;
  }
}
