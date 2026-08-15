package com.endcy.ai.guardrail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 护栏组件集成测试。
 *
 * @author endcy
 * @since 2026/08/08
 */
class GuardrailIntegrationTest {

    @Test
    void testToolValidator_PassValidInput() {
        String result = ToolValidator.validate("queryChargingStation", "{\"query\":\"SZ001\"}");
        assertNull(result, "合法参数应该通过");
    }

    @Test
    void testToolValidator_BlockScriptInjection() {
        String result = ToolValidator.validate("searchWeb", "{\"query\":\"<script>alert(1)</script>\"}");
        assertNotNull(result, "脚本注入应该被拦截");
        assertTrue(result.contains("脚本"));
    }

    @Test
    void testToolValidator_BlockSqlInjection() {
        String result = ToolValidator.validate("callHttpApi", "{\"url\":\"http://test.com'; DROP TABLE users;--\"}");
        assertNotNull(result, "SQL注入应该被拦截");
    }

    @Test
    void testToolValidator_BlockDirectoryTraversal() {
        String result = ToolValidator.validate("readFile", "{\"path\":\"../../etc/passwd\"}");
        assertNotNull(result, "目录穿越应该被拦截");
        assertTrue(result.contains("目录穿越"));
    }

    @Test
    void testToolValidator_BlockTooLongInput() {
        String longInput = "{\"query\":\"" + "a".repeat(10001) + "\"}";
        String result = ToolValidator.validate("searchWeb", longInput);
        assertNotNull(result, "超长参数应该被拦截");
    }

    @Test
    void testToolValidator_AllowEmptyInput() {
        String result = ToolValidator.validate("doTerminate", "");
        assertNull(result, "空参数应该通过");
    }

    @Test
    void testGuardrailResult_Block() {
        GuardrailResult result = GuardrailResult.block("测试拦截", "已拦截");
        assertTrue(result.isBlocked());
        assertEquals("已拦截", result.getPresetResponse());
    }

    @Test
    void testGuardrailResult_Redact() {
        GuardrailResult result = GuardrailResult.redact("PII脱敏", "138****1234");
        assertTrue(result.isRedacted());
        assertEquals("138****1234", result.getRedactedContent());
    }

    @Test
    void testGuardrailResult_Pass() {
        GuardrailResult result = GuardrailResult.pass();
        assertFalse(result.isBlocked());
        assertFalse(result.isRedacted());
    }

    @Test
    void testPiiRedactor_PhoneNumber() {
        PiiRedactor redactor = new PiiRedactor();
        GuardrailResult result = redactor.check("我的手机号是13812345678", java.util.Collections.emptyList());
        assertTrue(result.isRedacted(), "手机号应该被脱敏");
        assertFalse(result.getRedactedContent().contains("13812345678"), "脱敏后不应包含完整手机号");
    }

    @Test
    void testPiiRedactor_IdCard() {
        PiiRedactor redactor = new PiiRedactor();
        GuardrailResult result = redactor.check("身份证号是110101199003071234", java.util.Collections.emptyList());
        assertTrue(result.isRedacted(), "身份证号应该被脱敏");
    }

    @Test
    void testPiiRedactor_NoPii() {
        PiiRedactor redactor = new PiiRedactor();
        GuardrailResult result = redactor.check("充电桩故障码E001是什么", java.util.Collections.emptyList());
        assertFalse(result.isRedacted(), "无PII内容不应脱敏");
    }
}
