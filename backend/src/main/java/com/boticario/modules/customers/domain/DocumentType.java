package com.boticario.modules.customers.domain;

/**
 * Tipos de documento de identidad usados en Colombia. NIT se incluye
 * porque un "cliente" puede ser una empresa (compra al por mayor), no
 * solo una persona natural.
 */
public enum DocumentType {
    CC,
    CE,
    TI,
    PASAPORTE,
    NIT
}
