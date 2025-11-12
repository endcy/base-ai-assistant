package com.assistant.service.common.executor;

import com.assistant.service.common.constant.GlobalConstant;
import org.springframework.beans.factory.config.Scope;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

/**
 * 线程上下文变量回收装饰器
 *
 * @author endcy
 * @since 2025/08/04 21:05:13
 */
public class ThreadScopeCleanerDecorator implements TaskDecorator {
    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } finally {
                // 清理 SimpleThreadScope
                Scope scope = new SimpleThreadScope();
                scope.remove(GlobalConstant.THREAD_BEAN_SCOPE);
            }
        };
    }
}
