package com.lj.record.spring.boot.core;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author lvlaotou
 */
@Aspect
@Order(LogRecordConstant.AOP_ORDER)
@Slf4j
@RequiredArgsConstructor
public class LogRecordAop {

    private final OperatorService operatorService;

    private final LogRecordService logRecordService;

    /**
     * Spring 参数解析器
     */
    private final StandardReflectionParameterNameDiscoverer discoverer = new StandardReflectionParameterNameDiscoverer();

    /**
     * SPEL 表达式解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();


    @Pointcut("@annotation(logRecord)")
    public void pointcut(LogRecord logRecord) {

    }

    @SneakyThrows
    @Around(value = "pointcut(logRecord)", argNames = "joinPoint,logRecord")
    public Object around(ProceedingJoinPoint joinPoint, LogRecord logRecord){
        AopRootObject.AopRootObjectBuilder builder = AopRootObject.builder();
        Object result = null;
        try {
            // 初始化环境变量线程上下文
            AopContext.initVariableThreadContext();
            result = joinPoint.proceed();
            return result;
        }catch (Throwable throwable){
            builder.errorMessage(throwable.getMessage());
            throw throwable;
        } finally {
            try {
                LinkedHashMap<String, Object> variable = AopContext.listVariable();
                // 清除线程上下文环境变量
                AopContext.clearVariableThreadContext();
                Object finalResult = result;
                AtomicReference<Operator> operator = new AtomicReference<>(operatorService.getOperator());
                CompletableFuture.runAsync(()->{
                    try{
                        LinkedHashMap<String, Object> params = getRequestParam(joinPoint);
                        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
                        Method method = methodSignature.getMethod();
                        builder.param(params).method(method);
                        if (finalResult != null){
                            builder.result(finalResult);
                        }
                        AopRootObject rootObject = builder.build();
                        AopEvaluationContext evaluationContext = new AopEvaluationContext(rootObject, discoverer, variable);
                        if (!CollectionUtils.isEmpty(variable)){
                            variable.forEach(evaluationContext::setVariable);
                        }
                        // 是否记录日志
                        boolean isRecord = true;
                        String conditionSPEL = logRecord.condition();
                        if (StringUtils.hasText(conditionSPEL)){
                            // 获取触发条件
                            isRecord = Boolean.TRUE.equals(parser.parseExpression(conditionSPEL).getValue(evaluationContext, Boolean.class));
                        }
                        if (isRecord){
                            String errorMessage = rootObject.getErrorMessage();
                            boolean isSuccess = !StringUtils.hasText(errorMessage);
                            String operatorSPEL = logRecord.operator();
                            if (StringUtils.hasText(operatorSPEL)){
                                operator.set(parser.parseExpression(operatorSPEL).getValue(evaluationContext, Operator.class));
                            }
                            String describe = null;
                            String describeSPEL = logRecord.describe();
                            if (StringUtils.hasText(describeSPEL)){
                                describe = parser.parseExpression(describeSPEL).getValue(evaluationContext, String.class);
                            }
                            if (!isSuccess){
                                String errorMessageSPEL = logRecord.errorMessage();
                                if (StringUtils.hasText(errorMessageSPEL)){
                                    errorMessage = parser.parseExpression(errorMessageSPEL).getValue(evaluationContext, String.class);
                                }
                            }
                            Record.RecordBuilder recordBuilder = Record.builder()
                                    .success(isSuccess)
                                    .operateType(logRecord.operateType())
                                    .operator(operator.get());
                            if (StringUtils.hasText(describe)){
                                recordBuilder.describe(describe);
                            }
                            if (StringUtils.hasText(errorMessage)){
                                recordBuilder.errorMessage(errorMessage);
                            }
                            String bizNoSPEL = logRecord.bizNo();
                            if (StringUtils.hasText(bizNoSPEL)){
                                String bizNo = parser.parseExpression(bizNoSPEL).getValue(evaluationContext, String.class);
                                recordBuilder.bizNo(bizNo);
                            }
                            // 执行记录
                            logRecordService.record(recordBuilder.build());
                        }
                    }catch (Exception e){
                        log.error("记录操作日志异常", e);
                    }
                });
            }catch (Exception e){
                log.error("记录操作日志异常", e);
            }
        }
    }

    private LinkedHashMap<String, Object> getRequestParam(JoinPoint joinPoint){
        Assert.notNull(joinPoint, "JoinPoint is null");
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = methodSignature.getParameterNames();
        Object[] parameterValues = joinPoint.getArgs();
        if (parameterNames != null && parameterNames.length > 0 && parameterValues != null && parameterValues.length == parameterNames.length){
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            for (int i = 0; i < parameterNames.length; i++) {
                params.put(parameterNames[i], parameterValues[i]);
            }
            return params;
        }
        return null;
    }
}
