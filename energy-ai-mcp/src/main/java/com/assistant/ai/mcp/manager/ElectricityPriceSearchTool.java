package com.assistant.ai.mcp.manager;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * 电价查询工具
 * mcp示例
 */
@Service
public class ElectricityPriceSearchTool {

    @Tool(description = "根据城市名称查询尖峰平谷电价，电网发布的基准电价查询")
    public String searchElectricityPrice(@ToolParam(description = "城市名称") String city) {
        try {
            return getElectricityPrice(city);
        } catch (Exception e) {
            return "search Electricity Price failed, " + e.getMessage();
        }
    }

    /**
     * 根据城市查询电价 仅做示例；还没找到好的api
     * 使用api接口调用mcp服务中定义为接口的工具
     */
    public String getElectricityPrice(String city) {
        //返回json格式数据，最好返回带属性说明的数据格式
        return "{'尖':0.84, '峰':0.84, '平':0.52, '谷':0.31}";
    }

}
