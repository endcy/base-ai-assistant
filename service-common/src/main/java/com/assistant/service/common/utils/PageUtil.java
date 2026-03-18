package com.assistant.service.common.utils;

import cn.hutool.core.collection.CollUtil;
import com.assistant.service.common.base.PageInfo;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public class PageUtil extends cn.hutool.core.util.PageUtil {

    /**
     * List 分页
     */
    public static <T> List<T> toPage(int page, int size, List<T> list) {
        int fromIndex = page * size;
        int toIndex = page * size + size;
        if (fromIndex > list.size()) {
            return CollUtil.newArrayList();
        } else if (toIndex >= list.size()) {
            return list.subList(fromIndex, list.size());
        } else {
            return list.subList(fromIndex, toIndex);
        }
    }

    /**
     * 自定义分页
     */
    public static <T> PageInfo<T> toPage(List<T> object, long totalElements) {
        PageInfo<T> page = new PageInfo<>();
        page.setContent(object);
        page.setTotalElements(totalElements);
        return page;
    }

    public static <T> Page<T> toMybatisPage(Pageable pageable) {
        return toMybatisPage(pageable, false);
    }

    public static <T> Page<T> toMybatisPage(Pageable pageable, boolean ignoreOrderBy) {
        Page<T> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
        if (!ignoreOrderBy) {
            for (Sort.Order order : pageable.getSort()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setAsc(order.isAscending());
                orderItem.setColumn(StringUtils.camelToUnderline(order.getProperty()));
                page.addOrder(orderItem);
            }
        }
        return page;
    }
}
