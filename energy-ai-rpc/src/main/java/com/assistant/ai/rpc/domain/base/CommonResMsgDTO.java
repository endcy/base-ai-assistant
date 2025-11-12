package com.assistant.ai.rpc.domain.base;


import com.assistant.ai.rpc.enums.ApiResStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 基类 返回处理设备消息规约
 * 最小投递对象单元
 *
 * @author endcy
 * @implSpec 其他实现需要继承 {@link CommonResMsgDTO} 并实现转换为实际数据对象
 * --这样做是为了方便json传输数据，规避不同数据传输统一格式化以及减少冗余jsonMsg重复内容
 * @since 2025/08/04 21:00:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CommonResMsgDTO<T> extends BaseResMsgDTO {
    private static final long serialVersionUID = 6555168977714964177L;

    /**
     * 命令参数
     */
    private T data;

    public static <T> CommonResMsgDTO<T> successDeviceRes(T data) {
        CommonResMsgDTO<T> commonResMsgDTO = new CommonResMsgDTO<>();
        commonResMsgDTO.setStatus(ApiResStatus.SUCCESS);
        commonResMsgDTO.setMsg(ApiResStatus.SUCCESS.getRemark());
        commonResMsgDTO.setData(data);
        return commonResMsgDTO;
    }

    public static <T> CommonResMsgDTO<T> failureDeviceRes(T data) {
        CommonResMsgDTO<T> commonResMsgDTO = new CommonResMsgDTO<>();
        commonResMsgDTO.setStatus(ApiResStatus.FAILURE);
        commonResMsgDTO.setMsg(ApiResStatus.FAILURE.getRemark());
        commonResMsgDTO.setData(data);
        return commonResMsgDTO;
    }

    /**
     * 一般不处理错误，由RCP异常捕获处理，暴露异常也方便重传
     *
     * @param msg .
     * @return .
     */
    public static <T> CommonResMsgDTO<T> errorDeviceRes(String msg) {
        CommonResMsgDTO<T> commonResMsgDTO = new CommonResMsgDTO<T>();
        commonResMsgDTO.setStatus(ApiResStatus.ERROR);
        commonResMsgDTO.setMsg(msg);
        return commonResMsgDTO;
    }

    public void successDeviceRes() {
        this.setStatus(ApiResStatus.SUCCESS);
        this.setMsg(ApiResStatus.SUCCESS.getRemark());
    }

    public void failureDeviceRes() {
        this.setStatus(ApiResStatus.FAILURE);
        this.setMsg(ApiResStatus.FAILURE.getRemark());
    }

    public void errorDeviceRes() {
        this.setStatus(ApiResStatus.ERROR);
        this.setMsg(ApiResStatus.ERROR.getRemark());
    }
}
