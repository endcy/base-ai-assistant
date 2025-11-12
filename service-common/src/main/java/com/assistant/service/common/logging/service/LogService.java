package com.assistant.service.common.logging.service;

import com.assistant.service.common.base.BaseService;
import com.assistant.service.common.base.PageInfo;
import com.assistant.service.common.logging.domain.Log;
import com.assistant.service.common.logging.service.dto.LogQueryParam;
import com.assistant.service.common.logging.service.dto.LogSmallDTO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.Set;

public interface LogService extends BaseService<Log> {

    String CACHE_KEY = "log";

    int insert(Log res);

    int updateById(Log res);

    int removeByIds(Set<Long> ids);

    /**
     * 查询数据分页
     *
     * @param query    条件
     * @param pageable 分页参数
     * @return PageInfo<LogRecord>
     */
    PageInfo queryAll(LogQueryParam query, Pageable pageable);

    /**
     * 查询所有数据不分页
     *
     * @param query 条件参数
     * @return List<LogRecord>
     */
    List<Log> queryAll(LogQueryParam query);

    Log findById(Long id);

    /**
     * 查询用户日志
     *
     * @param criteria 查询条件
     * @param pageable 分页参数
     * @return -
     */
    PageInfo<LogSmallDTO> queryAllByUser(LogQueryParam criteria, Pageable pageable);

    /**
     * 保存日志数据
     *
     * @param username  用户
     * @param browser   浏览器
     * @param ip        请求IP
     * @param joinPoint /
     * @param log       日志实体
     */
    @Async("commonTaskExecutor")
    void save(String username, String browser, String ip, ProceedingJoinPoint joinPoint, Log log);

    /**
     * 查询异常详情
     *
     * @param id 日志ID
     * @return Object
     */
    Object findByErrDetail(Long id);

    boolean removeByLogType(String logType);

    /**
     * 删除所有错误日志
     */
    boolean delAllByError();

    /**
     * 删除所有INFO日志
     */
    boolean delAllByInfo();
}
