package com.lj.record.spring.boot.core;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lvlaotou
 */
@Slf4j
public class DefaultLogRecordServiceImpl implements LogRecordService {

    @Override
    public void record(Record record) {
        Operator operator = record.getOperator();
        String operatorName = null;
        String operatorId = null;
        if (operator != null){
            operatorName = operator.getName();
            operatorId = operator.getId();
        }
        log.info("{}【{}】 执行了 【{}】 业务编号【{}】 操作类型【{}】 操作是否成功【{}】", operatorName, operatorId, record.getDescribe(),
                record.getBizNo(), record.getOperateType(), record.getSuccess());
    }
}
