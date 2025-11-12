package com.assistant.ai.rpc.domain.base;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.assistant.ai.rpc.enums.ApiResStatus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * 基类 返回消息规约
 * 涉及网络传输，字段和实体越简约越高效
 *
 * @author endcy
 * @since 2025/08/04 21:00:00
 */
@Slf4j
@Data
public abstract class BaseResMsgDTO implements Serializable {
    private static final long serialVersionUID = -1818800438617041586L;

    /**
     * 状态
     */
    private ApiResStatus status;

    /**
     * 返回消息提示
     */
    private String msg;

    /**
     * 时间戳
     */
    private Long ts;

    /**
     * 返回消息json格式
     */
    private String jsonMsg;

    /**
     * 构造实例，初始化时间戳本身不随业务变更
     */
    protected BaseResMsgDTO() {
        this.ts = System.currentTimeMillis();
    }

    /**
     * 转换对象，转为任意类型
     * 只能用JSON序列化和反序列化的方式来转换对象
     *
     * @param clazz .
     * @param <T>   .
     * @return .
     */
    public <T extends BaseResMsgDTO> T convertToBean(String jsonMsg, Class<T> clazz) {
        try {
            if (jsonMsg == null) {
                return BeanUtil.copyProperties(this, clazz);
            }
            return JSONUtil.toBean(jsonMsg, clazz);
        } catch (Exception e) {
            log.error("data convert error, msg:{}", this);
            throw new RuntimeException("data convert error", e);
        }
    }

    /**
     * 设置json字符串
     * 用于转换基类对象为实现实体类
     */
    public String convertToJsonMsg() {
        return JSONUtil.toJsonStr(this);
    }

}
