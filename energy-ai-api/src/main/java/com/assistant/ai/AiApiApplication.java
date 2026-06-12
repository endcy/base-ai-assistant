package com.assistant.ai;

//import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;

import com.assistant.service.common.config.CommonServiceBeanConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@EnableAsync
//@EnableApolloConfig
@RestController
@SpringBootApplication
@EnableTransactionManagement
//非主包路径下的配置类@ComponentScan扫描bean，需要用@Import引入；要么就在Application类上@ComponentScan各bean路径
@Import(CommonServiceBeanConfig.class)
// Dubbo/Feign/Nacos 注册发现由 RpcComponentConfig 条件化启用（ai.rpc.enabled=true 时生效）
public class AiApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApiApplication.class, args);
        log.info("AiApiApplication boot started");
    }

    @GetMapping("/")
    public String healthCheck() {
        return "AiApiApplication started";
    }

}
