package com.assistant.service.common.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.Collection;

public interface CommonMapper<T> extends BaseMapper<T> {

    Integer insertBatchSomeColumn(Collection<T> entityList);

}
