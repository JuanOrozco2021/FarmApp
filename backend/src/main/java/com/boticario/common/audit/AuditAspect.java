package com.boticario.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aspecto AOP de la Fase 10: intercepta todo metodo anotado con
 * {@link Audited} y deja un registro en audit_logs, sin que cada servicio
 * tenga que llamar nada explicitamente.
 * <p>
 * Decisiones de diseno importantes:
 * <ul>
 *   <li>Solo se dispara con {@code @AfterReturning} — es decir, unicamente
 *   cuando el metodo termina exitosamente. Una accion que lanza una
 *   excepcion (validacion fallida, regla de negocio violada, etc.) nunca
 *   genera una fila de auditoria a medias.</li>
 *   <li>La escritura del log de auditoria NUNCA puede tumbar ni revertir
 *   la transaccion de negocio: si hay una transaccion Spring activa en
 *   curso, el guardado se difiere con {@code afterCommit} — asi, si la
 *   transaccion de negocio termina revirtiendose por cualquier motivo
 *   despues de que el metodo ya habia retornado (poco comun, pero
 *   posible), nunca queda un registro de auditoria de algo que en
 *   realidad no se aplico. Si no hay transaccion activa (por ejemplo en
 *   un test sin @Transactional), se guarda de inmediato.</li>
 *   <li>Cualquier error al construir o guardar el registro se atrapa y
 *   solo se deja un warning en el log — la auditoria jamas debe ser la
 *   causa de que una operacion de negocio falle.</li>
 * </ul>
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditLogRepository auditLogRepository,
                        CurrentUserProvider currentUserProvider,
                        ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
    }

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void afterAuditedAction(JoinPoint joinPoint, Audited audited, Object result) {
        try {
            AuditLog entry = buildEntry(joinPoint, audited, result);
            persistAfterCommitIfPossible(entry);
        } catch (Exception e) {
            log.warn("No se pudo preparar el registro de auditoria para {} sobre {}: {}",
                    audited.action(), audited.entity(), e.toString());
        }
    }

    private void persistAfterCommitIfPossible(AuditLog entry) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeSave(entry);
                }
            });
        } else {
            safeSave(entry);
        }
    }

    private void safeSave(AuditLog entry) {
        try {
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("No se pudo guardar el registro de auditoria {} sobre {} #{}: {}",
                    entry.getAction(), entry.getEntityName(), entry.getEntityId(), e.toString());
        }
    }

    private AuditLog buildEntry(JoinPoint joinPoint, Audited audited, Object result) {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();

        AuditLog entry = new AuditLog();
        entry.setAction(audited.action());
        entry.setEntityName(audited.entity());
        entry.setEntityId(resolveEntityId(result, args));
        entry.setUserId(currentUserProvider.currentUserId().orElse(null));
        entry.setUsername(currentUserProvider.currentUsername());
        entry.setOccurredAt(Instant.now());
        entry.setBeforeJson(toJsonSafely(buildParamsMap(paramNames, args)));
        entry.setAfterJson(toJsonSafely(result));
        return entry;
    }

    private Map<String, Object> buildParamsMap(String[] paramNames, Object[] args) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (paramNames == null) {
            return params;
        }
        for (int i = 0; i < paramNames.length && i < args.length; i++) {
            Object arg = args[i];
            if (arg == null || arg instanceof Pageable || arg instanceof Authentication) {
                continue;
            }
            if (isJpaEntity(arg)) {
                // Nunca se serializa una entidad JPA directamente (riesgo de proxies
                // perezosos/colecciones lazy fuera de sesion) — solo su id, si se
                // puede resolver.
                String id = tryExtractId(arg);
                params.put(paramNames[i], id != null ? ("id=" + id) : arg.getClass().getSimpleName());
            } else {
                params.put(paramNames[i], arg);
            }
        }
        return params;
    }

    private String resolveEntityId(Object result, Object[] args) {
        String id = tryExtractId(result);
        if (id != null) {
            return id;
        }
        for (Object arg : args) {
            if (arg instanceof Long value) {
                return String.valueOf(value);
            }
        }
        for (Object arg : args) {
            id = tryExtractId(arg);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    private boolean isJpaEntity(Object obj) {
        return obj.getClass().isAnnotationPresent(Entity.class);
    }

    private String tryExtractId(Object obj) {
        if (obj == null) {
            return null;
        }
        Method accessor = findAccessor(obj.getClass(), "getId");
        if (accessor == null) {
            accessor = findAccessor(obj.getClass(), "id");
        }
        if (accessor == null) {
            return null;
        }
        try {
            Object value = accessor.invoke(obj);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Method findAccessor(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private String toJsonSafely(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("No se pudo serializar un valor para auditoria: {}", e.toString());
            return null;
        }
    }
}
