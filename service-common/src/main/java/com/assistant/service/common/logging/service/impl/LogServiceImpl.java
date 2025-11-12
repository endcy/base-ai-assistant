package com.assistant.service.common.logging.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.assistant.service.common.annotation.LogRecord;
import com.assistant.service.common.base.PageInfo;
import com.assistant.service.common.base.impl.BaseServiceImpl;
import com.assistant.service.common.logging.domain.Log;
import com.assistant.service.common.logging.service.LogService;
import com.assistant.service.common.logging.service.dto.LogErrorDTO;
import com.assistant.service.common.logging.service.dto.LogQueryParam;
import com.assistant.service.common.logging.service.dto.LogSmallDTO;
import com.assistant.service.common.logging.service.mapper.LogMapper;
import com.assistant.service.common.utils.ConvertUtil;
import com.assistant.service.common.utils.PageUtil;
import com.assistant.service.common.utils.QueryHelpMybatisPlus;
import com.assistant.service.common.utils.ValidationUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class LogServiceImpl extends BaseServiceImpl<Log> implements LogService {

    private final LogMapper logMapper;

    @Override
    public int insert(Log res) {
        return logMapper.insert(res);
    }

    @Override
    public int updateById(Log res) {
        return logMapper.updateById(res);
    }

    @Override
    public int removeByIds(Set<Long> ids) {
        return logMapper.deleteByIds(ids);
    }

    private void setDefaultCreateTime(LogQueryParam query) {
        if (CollUtil.isEmpty(query.getCreateTime())) {
            DateTime now = DateUtil.date();
            // 查询最近6个月系统日志
            query.setCreateTime(Arrays.asList(DateUtil.offsetMonth(now, -6), DateUtil.offsetMinute(now, 1)));
        }
    }

    @Override
    public PageInfo queryAll(LogQueryParam query, Pageable pageable) {
        setDefaultCreateTime(query);
        IPage<Log> page = PageUtil.toMybatisPage(pageable);
        IPage<Log> pageList = logMapper.selectPage(page, QueryHelpMybatisPlus.getPredicate(query));
        String status = "ERROR";
        if (status.equals(query.getLogType())) {
            return ConvertUtil.convertPage(pageList, LogErrorDTO.class);
        }
        return ConvertUtil.convertPage(pageList, Log.class);
    }

    @Override
    public List<Log> queryAll(LogQueryParam query) {
        setDefaultCreateTime(query);
        return logMapper.selectList(QueryHelpMybatisPlus.getPredicate(query));
    }

    @Override
    public PageInfo<LogSmallDTO> queryAllByUser(LogQueryParam query, Pageable pageable) {
        setDefaultCreateTime(query);
        IPage<Log> page = PageUtil.toMybatisPage(pageable);
        IPage<Log> pageList = logMapper.selectPage(page, QueryHelpMybatisPlus.getPredicate(query));
        return ConvertUtil.convertPage(pageList, LogSmallDTO.class);
    }

    @Override
    public Log findById(Long id) {
        return logMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByLogType(String logType) {
        UpdateWrapper<Log> wrapper = new UpdateWrapper<>();
        wrapper.lambda().eq(Log::getLogType, logType);
        return logMapper.delete(wrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(String username, String browser, String ip, ProceedingJoinPoint joinPoint, Log log1) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 方法路径
        String methodName = joinPoint.getTarget().getClass().getSimpleName() + "." + signature.getName() + "()";

        StringBuilder params = new StringBuilder();
        //参数值
        List<Object> argValues = new ArrayList<>(Arrays.asList(joinPoint.getArgs()));
        //参数名称
        for (Object argValue : argValues) {
            params.append(argValue).append(" ");
        }
        // 描述
        LogRecord aopLogRecord = method.getAnnotation(LogRecord.class);
        if (log1 != null) {
            log1.setDescription(aopLogRecord.value());
        }
        assert log1 != null;
        log1.setRequestIp(ip);

        String loginPath = "login";
        if (loginPath.equals(signature.getName())) {
            try {
                Object usernameStr = new JSONObject(argValues.getFirst()).get("username");
                if (ObjectUtil.isNotEmpty(usernameStr)) {
                    username = usernameStr.toString();
                }
            } catch (Exception e) {
                LogServiceImpl.log.error(e.getMessage(), e);
            }
        }
        //log1.setAddress(StringUtils.getCityInfo(log1.getRequestIp()));
        log1.setMethod(methodName);
        log1.setUsername(username);
        log1.setParams("{" + params + " }");
        log1.setBrowser(browser);
        log.info("log-aspect ip[{}] method[{}] user[{}] param[{}]", ip, methodName, username, params);
        //setExceptionDetail仅保留2000个字符
        if (log1.getExceptionDetail() != null) {
            String tmp = new String(log1.getExceptionDetail());
            log1.setExceptionDetail(tmp.length() > 2000 ? tmp.substring(0, 2000).getBytes() : tmp.getBytes());
        }
        if (log1.getDescription() != null) {
            log1.setDescription(log1.getDescription().length() > 2000 ? log1.getDescription().substring(0, 2000) : log1.getDescription());
        }
        if (log1.getId() == null) {
            log1.setCreateTime(new Date());
            logMapper.insert(log1);
        } else {
            logMapper.updateById(log1);
        }
    }

    @Override
    public Object findByErrDetail(Long id) {
        Log log = findById(id);
        ValidationUtil.isNull(log.getId(), "LogRecord", "id", id);
        byte[] details = log.getExceptionDetail();
        return Dict.create().set("exception", new String(ObjectUtil.isNotNull(details) ? details : "".getBytes()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delAllByError() {
        return this.removeByLogType("ERROR");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delAllByInfo() {
        return this.removeByLogType("INFO");
    }
}
