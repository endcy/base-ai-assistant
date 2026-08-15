package com.endcy.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.endcy.ai.manager.OssUploadManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 生成工具 —— 把结构化数据生成 Excel 并上传 OSS 返回下载 URL。
 *
 * <p>支持的数据格式：
 * <ul>
 *   <li>JSON 数组字符串：{@code [{"name":"张三","age":30},{"name":"李四","age":25}]}</li>
 *   <li>表头 + 数据（二维数组）：通过 columns 和 rows 参数传入</li>
 * </ul>
 *
 * @author endcy
 * @since 2026-08-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExcelGenerationTool {

    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final OssUploadManager ossUploadManager;

    @Tool(description = "将结构化数据生成 Excel 文件并上传到 OSS，返回可下载的 URL。" +
            "适用场景：把查询结果、统计数据、列表数据导出为 Excel 文件给用户下载。" +
            "支持两种数据格式：(1) JSON 数组字符串；(2) 列名 + 多行数据的表格形式。")
    public String generateExcel(
            @ToolParam(description = "Excel 文件名（不含扩展名），如'电价统计'、'站点清单'") String fileName,
            @ToolParam(description = "JSON 数组格式的数据。例：[{\"name\":\"张三\",\"age\":30},{\"name\":\"李四\",\"age\":25}]。" +
                    "如果数据已经以 JSON 数组形式存在，直接传入此参数即可。") String jsonData,
            @ToolParam(description = "工作表名称，默认 'Sheet1'") String sheetName) {
        if (!ossUploadManager.isAvailable()) {
            return "错误：OSS 上传服务未配置，无法生成可下载文件";
        }
        if (StrUtil.isBlank(jsonData)) {
            return "错误：数据为空";
        }

        try {
            JSONArray arr = JSON.parseArray(jsonData);
            if (arr == null || arr.isEmpty()) {
                return "错误：解析后数据为空";
            }

            // 提取表头（取第一个对象的所有 key）
            List<String> headers = new ArrayList<>();
            JSONObject first = arr.getJSONObject(0);
            for (String key : first.keySet()) {
                headers.add(key);
            }

            // 转换为 EasyExcel 需要的 List<List<Object>> 格式
            List<List<Object>> rows = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                List<Object> row = new ArrayList<>();
                for (String header : headers) {
                    row.add(obj.get(header));
                }
                rows.add(row);
            }

            String sheet = StrUtil.blankToDefault(sheetName, "Sheet1");
            String file = StrUtil.blankToDefault(fileName, "数据导出") + ".xlsx";

            // 生成 Excel 字节流
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            EasyExcel.write(out)
                     .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                     .head(buildHead(headers))
                     .sheet(sheet)
                     .doWrite(rows);

            // 上传到 OSS
            String url = ossUploadManager.uploadBytes(out.toByteArray(), file, EXCEL_CONTENT_TYPE);
            return String.format("Excel 文件已生成并上传：%s\n共 %d 行数据，%d 列\n文件名：%s",
                    url, rows.size(), headers.size(), file);
        } catch (Exception e) {
            log.error("Excel 生成失败: {}", e.getMessage(), e);
            return "Excel 生成失败：" + e.getMessage();
        }
    }

    @Tool(description = "将表头和行数据生成 Excel 文件并上传到 OSS，返回下载 URL。" +
            "适合数据不是 JSON 格式、需要显式指定列名和每行数据的场景。")
    public String generateExcelFromRows(
            @ToolParam(description = "文件名（不含扩展名）") String fileName,
            @ToolParam(description = "列名数组，JSON 数组格式。例：[\"姓名\",\"年龄\",\"城市\"]") String columnsJson,
            @ToolParam(description = "行数据，JSON 二维数组格式。例：[[\"张三\",30,\"深圳\"],[\"李四\",25,\"北京\"]]") String rowsJson,
            @ToolParam(description = "工作表名称") String sheetName) {
        if (!ossUploadManager.isAvailable()) {
            return "错误：OSS 上传服务未配置";
        }
        try {
            List<String> headers = JSON.parseArray(columnsJson, String.class);
            JSONArray rowsArr = JSON.parseArray(rowsJson);
            if (headers == null || headers.isEmpty() || rowsArr == null || rowsArr.isEmpty()) {
                return "错误：表头或数据为空";
            }

            List<List<Object>> rows = new ArrayList<>();
            for (int i = 0; i < rowsArr.size(); i++) {
                JSONArray rowArr = rowsArr.getJSONArray(i);
                List<Object> row = new ArrayList<>();
                for (int j = 0; j < rowArr.size(); j++) {
                    row.add(rowArr.get(j));
                }
                rows.add(row);
            }

            String sheet = StrUtil.blankToDefault(sheetName, "Sheet1");
            String file = StrUtil.blankToDefault(fileName, "数据导出") + ".xlsx";

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            EasyExcel.write(out)
                     .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                     .head(buildHead(headers))
                     .sheet(sheet)
                     .doWrite(rows);

            String url = ossUploadManager.uploadBytes(out.toByteArray(), file, EXCEL_CONTENT_TYPE);
            return String.format("Excel 已生成并上传：%s\n共 %d 行数据，%d 列\n文件名：%s",
                    url, rows.size(), headers.size(), file);
        } catch (Exception e) {
            log.error("Excel 生成失败: {}", e.getMessage(), e);
            return "Excel 生成失败：" + e.getMessage();
        }
    }

    /**
     * EasyExcel 表头格式：List<List<String>>，每个子 list 是一列的表头（支持多级表头，这里单级）
     */
    private List<List<String>> buildHead(List<String> headers) {
        List<List<String>> head = new ArrayList<>();
        for (String h : headers) {
            List<String> col = new ArrayList<>();
            col.add(h);
            head.add(col);
        }
        return head;
    }
}
