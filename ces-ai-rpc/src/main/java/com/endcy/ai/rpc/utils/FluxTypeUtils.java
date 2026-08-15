package com.endcy.ai.rpc.utils;

import reactor.core.publisher.Flux;

/**
 * Flux类型转换工具
 *
 * @author endcy
 * @since 2025/12/15 17:03:54
 */
public class FluxTypeUtils {
    public static <T> Flux<T> safeCastFlux(Flux<?> originalFlux, Class<T> targetClass) {
        return originalFlux.filter(targetClass::isInstance)
                           .map(targetClass::cast);
    }

    @SuppressWarnings("unchecked")
    public static <T> Flux<T> unsafeCastFlux(Flux<?> originalFlux) {
        return (Flux<T>) originalFlux;
    }
}
