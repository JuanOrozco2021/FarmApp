package com.boticario.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un metodo de un servicio de aplicacion como una accion de negocio
 * que debe quedar registrada en audit_logs (Fase 10: "auditoria completa").
 * La intercepta {@link AuditAspect} via AOP — no hay que llamar nada
 * manualmente desde el servicio.
 * <p>
 * Convenciones de extraccion (ver AuditAspect para el detalle):
 * <ul>
 *   <li>{@code entityId} se intenta primero desde el valor de retorno
 *   (campo/metodo {@code id}), y si no aparece ahi, desde los argumentos
 *   del metodo (el primer {@code Long}, o el id de un argumento que sea
 *   una entidad JPA).</li>
 *   <li>{@code before_json} = los argumentos de la llamada (la solicitud/
 *   intencion), nunca el estado previo de la entidad en base de datos —
 *   ver el comentario de {@link AuditAspect} para la justificacion.</li>
 *   <li>{@code after_json} = el valor de retorno (el resultado tal como lo
 *   ve el cliente).</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {

    /** Codigo corto de la accion, ej. "PRODUCT_CREATE", "SALE_CANCEL". */
    String action();

    /** Nombre logico de la entidad afectada, ej. "Product", "Sale". */
    String entity();
}
