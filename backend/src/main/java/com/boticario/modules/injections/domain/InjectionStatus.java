package com.boticario.modules.injections.domain;

/**
 * ACTIVA: el servicio se aplico y su cobro esta vigente. ANULADA: se
 * anulo el cobro (por ejemplo, un error de registro) — el registro
 * original nunca se borra ni se reescribe, igual criterio que Sale.
 */
public enum InjectionStatus {
    ACTIVA,
    ANULADA
}
