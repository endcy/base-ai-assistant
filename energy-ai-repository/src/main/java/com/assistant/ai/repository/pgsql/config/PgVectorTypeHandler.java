package com.assistant.ai.repository.pgsql.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

/**
 * ...
 *
 * @author endcy
 * @date 2025/8/6 20:57:20
 */
@MappedTypes(Object.class)
public class PgVectorTypeHandler extends BaseTypeHandler<Object> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        // 以PG自定义类型存储
        ps.setObject(i, parameter, Types.OTHER);
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        // 直接返回PG向量对象
        return rs.getObject(columnName);
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return null;
    }
}
