package io.github.lvlaotou.log.record.log.record.spring.boot.autoconfigure;

import io.github.lvlaotou.log.record.log.record.spring.boot.core.LogRecordService;
import io.github.lvlaotou.log.record.log.record.spring.boot.core.DefaultLogRecordServiceImpl;
import io.github.lvlaotou.log.record.log.record.spring.boot.core.LogRecordAop;
import io.github.lvlaotou.log.record.log.record.spring.boot.core.OperatorService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class LogRecordAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LogRecordService logRecordService(){
        return new DefaultLogRecordServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public LogRecordAop logRecordAop(LogRecordService logRecordService,
                                     OperatorService operatorService){
        return new LogRecordAop(operatorService, logRecordService);
    }
}
