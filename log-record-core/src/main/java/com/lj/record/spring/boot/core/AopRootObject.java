package com.lj.record.spring.boot.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;

/**
 * @author lvlaotou
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AopRootObject {

    private LinkedHashMap<String, Object> param;

    private Method method;

    private Object result;

    private String errorMessage;

    public Object[] getArgs(){
        return CollectionUtils.isEmpty(param) ? null : param.values().toArray();
    }
}
