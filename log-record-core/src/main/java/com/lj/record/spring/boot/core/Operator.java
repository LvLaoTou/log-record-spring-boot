package com.lj.record.spring.boot.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 日志操作者
 * @author lvlaotou
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Operator {

    /** id */
    private String id;

    /** 用户名 */
    private String name;
}
