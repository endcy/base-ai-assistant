package com.assistant.service.common.constant;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;

import java.util.Date;

/**
 * 常用静态常量
 *
 * @author endcy
 * @since 2025/08/04 21:05:13
 */
public interface GlobalConstant {


    /**
     * win 系统
     */
    String WIN = "win";

    /**
     * mac 系统
     */
    String MAC = "mac";

    /**
     * 替代线程变量的bean scope标识
     */
    String THREAD_BEAN_SCOPE = "thread";

    /**
     * 默认的文档过期时间（远未来时间）
     */
    Date DEFAULT_EXPIRED_DATE = DateUtil.parse("2099-12-31 23:59:59.999", DatePattern.NORM_DATETIME_MS_PATTERN);

    /**
     * 文档外部id标识 文档元数据
     */
    String DOC_ID_MARK = "id";
    String DOC_TITLE_MARK = "title";
    String DOC_SCOPE_MARK = "sourceType";
    String DOC_BUSINESS_MARK = "businessType";
}
