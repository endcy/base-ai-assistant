package com.assistant.service.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.assistant.service.common.annotation.Query;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;

@Slf4j
public class QueryHelpMybatisPlus {

    public static <R, Q> QueryWrapper<R> getPredicate(Q query, String orderBy, boolean asc) {
        QueryWrapper<R> wrapper = getPredicate(query);
        if (StrUtil.isBlank(orderBy)) {
            return wrapper;
        }
        if (asc) {
            wrapper.orderByAsc(orderBy);
        } else {
            wrapper.orderByDesc(orderBy);
        }
        return wrapper;
    }

    public static <R, Q> QueryWrapper<R> getPredicateSimple(Q query) {
        QueryWrapper<R> wrapper = getPredicate(query);
        wrapper.orderByDesc("id");
        return wrapper;
    }

    public static <R, Q> QueryWrapper<R> getPredicate(Q query, String groupByColumn) {
        QueryWrapper<R> wrapper = getPredicate(query);
        if (StrUtil.isBlank(groupByColumn)) {
            return wrapper;
        }
        wrapper.groupBy(groupByColumn);
        return wrapper;
    }

    public static <R, Q> QueryWrapper<R> getPredicate(Q query) {
        QueryWrapper<R> queryWrapper = new QueryWrapper<>();
        if (query == null) {
            return queryWrapper;
        }

        try {
            List<Field> fields = getAllFields(query.getClass(), new ArrayList<>());

            //目标类的字段名
            for (Field field : fields) {
                boolean accessible = field.isAccessible();
                field.setAccessible(true);
                Query q = field.getAnnotation(Query.class);
                if (q == null) {
                    field.setAccessible(accessible);
                    continue;
                }
                String propName = q.propName();
                String blurry = q.blurry();
                String blurryEq = q.blurryEq();
                String alias = q.tableAlias();
                String queryAlias = StrUtil.isEmpty(alias) ? "" : alias + ".";
                String attributeName = StrUtil.isBlank(propName) ? field.getName() : propName;
                attributeName = queryAlias + humpToUnderline(attributeName);
                Object val = field.get(query);
                if (ObjectUtil.isNull(val) || "".equals(val) || ObjectUtil.isEmpty(val)) {
                    continue;
                }
                // 模糊多字段
                if (ObjectUtil.isNotEmpty(blurry)) {
                    String[] blurrys = blurry.split(",");
                    queryWrapper.and(wrapper -> {
                        for (String blurry1 : blurrys) {
                            String column = queryAlias + humpToUnderline(blurry1);
                            wrapper.or();
                            wrapper.like(column, val.toString());
                        }
                    });
                    continue;
                }
                if (ObjectUtil.isNotEmpty(blurryEq)) {
                    String[] blurrys = blurryEq.split(",");
                    queryWrapper.and(wrapper -> {
                        for (String blurry1 : blurrys) {
                            String column = queryAlias + humpToUnderline(blurry1);
                            wrapper.or();
                            wrapper.eq(column, val.toString());
                        }
                    });
                    continue;
                }
                String finalAttributeName = attributeName;
                switch (q.type()) {
                    case EQUAL:
                        queryWrapper.eq(attributeName, val);
                        break;
                    case GREATER_THAN:
                        queryWrapper.gt(finalAttributeName, val);
                        break;
                    case GREATER_THAN_EQ:
                        queryWrapper.ge(finalAttributeName, val);
                        break;
                    case LESS_THAN:
                        queryWrapper.lt(finalAttributeName, val);
                        break;
                    case LESS_THAN_EQ:
                        queryWrapper.le(finalAttributeName, val);
                        break;
                    case INNER_LIKE:
                        queryWrapper.like(finalAttributeName, val);
                        break;
                    case LEFT_LIKE:
                        queryWrapper.likeLeft(finalAttributeName, val);
                        break;
                    case RIGHT_LIKE:
                        queryWrapper.likeRight(finalAttributeName, val);
                        break;
                    case NOT_LIKE:
                        queryWrapper.notLike(finalAttributeName, val);
                        break;
                    case NOT_IN:
                        if (CollUtil.isNotEmpty((Collection<Long>) val)) {
                            queryWrapper.notIn(finalAttributeName, (Collection<Long>) val);
                        }
                        break;
                    case IN:
                        if (CollUtil.isNotEmpty((Collection<Long>) val)) {
                            queryWrapper.in(finalAttributeName, (Collection<Long>) val);
                        } else {
                            queryWrapper.in(finalAttributeName, 999999L);
                        }
                        break;
                    case IN_SQL: {
                        String sql = q.sql();
                        sql = StrUtil.replace(sql, "?", val.toString());
                        queryWrapper.inSql(finalAttributeName, sql);
                    }
                    break;
                    case NOT_EQUAL:
                        queryWrapper.ne(finalAttributeName, val);
                        break;
                    case NOT_NULL:
                        queryWrapper.isNotNull(finalAttributeName);
                        break;
                    case IS_NULL:
                        queryWrapper.isNull(finalAttributeName);
                        break;
                    case BETWEEN:
                        List<Object> between = new ArrayList<>((List<Object>) val);
                        queryWrapper.between(finalAttributeName, between.get(0), between.get(1));
                        break;
                    case NOT_BETWEEN:
                        List<Object> notBetween = new ArrayList<>((List<Object>) val);
                        queryWrapper.notBetween(finalAttributeName, notBetween.get(0), notBetween.get(1));
                        break;
                    case ANY_LIKE:
                        List<String> anyLikeList = new ArrayList<>((List<String>) val);
                        if (ArrayUtil.isEmpty(anyLikeList)) {
                            break;
                        }
                        queryWrapper.and(
                                wrapper -> {
                                    for (int i = 0; i < anyLikeList.size(); i++) {
                                        wrapper.like(finalAttributeName, anyLikeList.get(i));
                                        if (i < anyLikeList.size() - 1) {
                                            wrapper.or();
                                        }
                                    }
                                }
                        );
                        break;
                    default:
                        break;
                }
                field.setAccessible(accessible);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return queryWrapper;
    }

    public static List<Field> getAllFields(Class clazz, List<Field> fields) {
        if (clazz != null) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            getAllFields(clazz.getSuperclass(), fields);
        }
        return fields;
    }

    /***
     * 驼峰命名转为下划线命名
     *
     * @param para
     *        驼峰命名的字符串
     */

    public static String humpToUnderline(String para) {
        StringBuilder sb = new StringBuilder(para);
        int temp = 0;
        if (!para.contains("_")) {
            for (int i = 0; i < para.length(); i++) {
                if (Character.isUpperCase(para.charAt(i))) {
                    sb.insert(i + temp, "_");
                    temp += 1;
                }
            }
        }
        return sb.toString();
    }

    public static String getSelectSql(Map<String, LambdaQueryWrapper<?>> tableAliasQueryWrappers) {
        String selectSql = "";
        for (Map.Entry<String, LambdaQueryWrapper<?>> entry : tableAliasQueryWrappers.entrySet()) {
            String tableAlias = entry.getKey();
            if (StrUtil.isBlank(tableAlias)) {
                continue;
            }
            LambdaQueryWrapper<?> queryWrapper = entry.getValue();
            String tmp = queryWrapper.getSqlSelect();
            if (StrUtil.isNotBlank(tmp)) {
                //逗号分隔拆分，并拼接表别名，最后再组合为一个sql片段
                List<String> columns = StrUtil.split(tmp, ",");
                for (int i = 0; i < columns.size(); i++) {
                    String column = columns.get(i);
                    if (StrUtil.isNotBlank(column)) {
                        columns.set(i, tableAlias + "." + column);
                    }
                }
                tmp = StrUtil.join(",", columns);
            } else {
                tmp = tableAlias + ".*";
            }
            if (StrUtil.isNotBlank(tmp)) {
                selectSql = StrUtil.isNotEmpty(selectSql) ? selectSql + "," + tmp : tmp;
            }
        }
        return selectSql;
    }

}
