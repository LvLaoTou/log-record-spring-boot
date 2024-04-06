package com.lj.record.spring.boot.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lvlaotou
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Record {

    /** 操作者 */
    private Operator operator;

    /** 操作类型 */
    private OperateTypeEnum operateType;

    /** 是否成功 */
    private Boolean success;

    /** 描述 */
    private String describe;

    /** 业务号 */
    private String bizNo;

    /** 错误信息 */
    private String errorMessage;
}
