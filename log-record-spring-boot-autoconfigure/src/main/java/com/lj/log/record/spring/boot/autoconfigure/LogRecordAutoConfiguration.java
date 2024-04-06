package com.lj.log.record.spring.boot.autoconfigure;

import com.lj.record.spring.boot.core.DefaultLogRecordServiceImpl;
import com.lj.record.spring.boot.core.LogRecordAop;
import com.lj.record.spring.boot.core.LogRecordService;
import com.lj.record.spring.boot.core.OperatorService;
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
