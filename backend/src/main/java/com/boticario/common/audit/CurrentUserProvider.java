package com.boticario.common.audit;

import java.util.Optional;

/**
 * Puerto para resolver el usuario autenticado actual. Vive en common (no en
 * identity) para que otros modulos (inventario, compras, ventas...) puedan
 * depender de esta interfaz sin acoplarse al modulo de identidad — identity
 * aporta la unica implementacion real.
 */
public interface CurrentUserProvider {

    /** Username tal como quedo en el JWT (o "system" fuera de una peticion autenticada). */
    String currentUsername();

    /** ID numerico del usuario autenticado, si existe en la base de datos. */
    Optional<Long> currentUserId();

    /**
     * Si el usuario autenticado actual tiene el permiso indicado (codigo de
     * la tabla permissions, ej. "SALE_APPLY_DISCOUNT"). Pensado para reglas
     * de negocio que dependen del permiso pero no ameritan partir el
     * endpoint en dos (ej. "el descuento en esta venta solo aplica si quien
     * la registra puede aplicar descuentos") — @PreAuthorize sigue siendo
     * la primera linea de defensa para endpoints que SIEMPRE requieren un
     * permiso fijo.
     */
    boolean hasAuthority(String authority);
}
