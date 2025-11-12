package com.assistant.service.common.config.mq;

import com.alibaba.mq.amqp.utils.UserUtils;
import com.rabbitmq.client.impl.CredentialsProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class AliyunCredentialsProvider implements CredentialsProvider {
    /**
     * Access Key ID.
     */
    private final String accessKeyId;
    /**
     * Access Key Secret.
     */
    private final String accessKeySecret;
    /**
     * security temp token. (optional)
     */
    private final String securityToken;
    /**
     * instanceId(实例Id，可从AMQP控制台首页获取)
     */
    private final String instanceId;

    public AliyunCredentialsProvider(final String accessKeyId, final String accessKeySecret,
                                     final String instanceId) {
        this(accessKeyId, accessKeySecret, null, instanceId);
    }

    public AliyunCredentialsProvider(final String accessKeyId, final String accessKeySecret,
                                     final String securityToken, final String instanceId) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.securityToken = securityToken;
        this.instanceId = instanceId;
    }

    @Override
    public String getUsername() {
        if (StringUtils.isNotEmpty(securityToken)) {
            return UserUtils.getUserName(accessKeyId, instanceId, securityToken);
        } else {
            return UserUtils.getUserName(accessKeyId, instanceId);
        }
    }

    @Override
    public String getPassword() {
        try {
            return UserUtils.getPassord(accessKeySecret);
        } catch (Exception e) {
            log.error(">>>>>>>>>>>>>>>>>>>>>>>>> MQ getPassword error", e);
        }
        return null;
    }
}
