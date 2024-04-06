## 项目介绍

> 注意：本项目是参考美团的技术文章实现的，原文看这里[如何优雅地记录操作日志？](https://tech.meituan.com/2021/09/16/operational-logbook.html)本项目不是原文的官方实现，官方实现在这里[mzt-biz-log](https://github.com/mouzt/mzt-biz-log/)。
> 

本项目基于Spring AOP实现通过一个自定义注解（`@LogRecord`）优雅的记录操作日志，并且参数支持SPEL表达式，以及AOP嵌套使用。

## 项目背景

我想大家一定有在项目中记录过操作日志吧！所以下面这个场景大家一定都不陌生：

| 序号 | 操作人 | 事件 | 操作时间 |
| --- | --- | --- | --- |
| 1 | 张三 | 修改用户【赵六】状态由【正常】改为【冻结】 | 2024-04-06 14:33:23 |
| 2 | 李四 | 新增公告【五一劳动节放假通知】 | 2024-04-06 09:11:45 |
| 3 | 王五 | 删除文章【如何使用Git工作流】 | 2024-04-06 08:55:08 |

上面表中的数据就是我们日常开发中最常见的操作日志，其功能就是记录「谁？」「在什么时间？」「做了什么事？」

而要实现上面的功能，我们普通的做法可能就是，在业务代码里面增加记录操作日志的代码。可能像下面这样：

```java
public void updateUserStatus(long userId, UserStatus status){
    User user = getUserById(userId);
		// 记录操作日志
		String log = UserContext.getCurrentUser()+"修改用户【"+user.getName()+"状态由【"+user.getStatus()+"】改为【"+status+"】"
		logService.save(log);
		// 修改用户状态
		user.setStatus(status);
		updateById(user);
}
```

这样做的缺点就是，日志和业务强绑定了，对业务入侵性太大，而且不便于维护。

因此，我们希望将记录操作日志从业务中剥离出来。首先考虑到的技术就是自定义注解+Spring AOP，实现后的效果大致如下：

```java
@LogRecord(describe ="#operator+'修改用户【'+#userName+'】状态由【'+#userStatus+'】改为【'+#status+'】'", operateType = OperateTypeEnum.UPDATE, bizNo="#userId")
public void updateUserStatus(long userId, UserStatus status){
    User user = getUserById(userId);
		// 修改用户状态
		user.setStatus(status);
		updateById(user);
}
```

而注解中的`#operator` 等参数则可以通过SPEL解析替换成实际的业务值。

## 项目特点

- 可以通过SPEL实现复杂日志的构建
- 支持自定义SPEL参数构建，解决记录日志需要的业务数据无法通过常规的方式传递SPEL参数
- 支持AOP嵌套使用
- 可自定义记录操作日志的方式
- 可自定义构建当前操作人
- 使用Springboot starter构建，引入依赖即可自动配置
- 更多特性正在持续更新中。。。

## 项目要求

- JDK17
- Springboot3.X

## 使用方法

1. 引入依赖

```java
<dependency>
    <groupId>com.lj.log</groupId>
    <artifactId>log-record-spring-boot-starter</artifactId>
    <version>${version}</version>
</dependency>
```

1. 「**必须**」实现`com.lj.record.spring.boot.core.OperatorService` 接口，并向Spring注册bean。此接口是为了获取操作人，参考：

```java
@Service
public class TestOperatorServiceImpl implements OperatorService {

    @Override
    public Operator getOperator() {
        return Operator.builder().id("1").name("admin").build();
    }
}
```

1. 「**建议**」实现`com.lj.record.spring.boot.core.LogRecordService` 接口，并向Spring注册bean。此接口是为了记录操作日志，提供一个默认实现，如下：

```java
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
```

1. 在需要记录操作日志的方法上使用`@LogRecord` 进行标注，参考：

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "测试请求参数对象")
public class TestRequest {

    @NotNull(message = "id不能为空")
    @Schema(description = "id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "名称不能为空")
    @Schema(description = "姓名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "是否开启日志记录", example = "true")
    private Boolean open;
}
```

```java
@Service
@RequiredArgsConstructor
public class TestService {

    private final TestService2 testService2;

    @LogRecord(describe = "'请求id:'+#request.id+',请求姓名:'+#request.name",
            operateType = OperateTypeEnum.UPDATE,
            bizNo = "T(java.util.UUID).randomUUID()",
            errorMessage = "'执行获取请求姓名失败'",
            condition = "#request.open")
    public String convert(TestRequest request){
        testService2.test(request);
        return request.getName();
    }
}
```

1. 「**可选**」支持嵌套使用

```java
@Service
public class TestService2 {
4
    @LogRecord(describe = "'请求id:'+#request.id+',请求姓名:'+#request.name",
            operateType = OperateTypeEnum.SELECT,
            bizNo = "T(java.util.UUID).randomUUID()",
            errorMessage = "'执行嵌套,错误信息:'+#"+ LogRecordConstant.ERROR_MESSAGE_EVALUATION,
            condition = "#request.open")
    public long test(TestRequest request){
        return request.getId();
    }
}
```

1. 「**可选**」当请求参数和返回值都没有记录日志需要的参数时，可以自定义设置上下文，参考如下：

```java
@Service
@RequiredArgsConstructor
public class TestService {

    private final TestService2 testService2;

    @LogRecord(describe = "'请求id:'+#request.id+',请求姓名:'+#request.name+',是否执行其他业务'+#other",
            operateType = OperateTypeEnum.UPDATE,
            bizNo = "T(java.util.UUID).randomUUID()",
            errorMessage = "'执行获取请求姓名失败'",
            condition = "#request.open")
    public String convert(TestRequest request){
		    // 向当前线程AOP上下文设置参数
		    AopContext.putVariable("other", true);
        testService2.test(request);
        return request.getName();
    }
}
https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop
```

## 常用对象

- com.lj.record.spring.boot.core.LogRecord 记录操作日志的注解，用于方法上，参数如下：

```java
public @interface LogRecord {

    /**
     * 操作描述
     * 支持spel表达式,spel表达式结果需要为String
     */
    String describe();

    /**
     * 业务号
     * 支持spel表达式,spel表达式结果需要为String
     */
    String bizNo() default "";

    /**
     * 操作类型
     */
    OperateTypeEnum operateType();

    /**
     * 错误信息
     * 支持spel表达式,spel表达式结果需要为String
     * 默认为{@link java.lang.Throwable#getMessage()}
     */
    String errorMessage() default "";

    /**
     * 操作者
     * 支持spel表达式,spel表达式结果需要为{@link Operator}
     */
    String operator() default "";

    /**
     * 触发条件
     * 支持spel表达式,spel表达式结果需要为boolean
     * 默认为true
     */
    String condition() default "";
}
```

- com.lj.record.spring.boot.core.OperateTypeEnum 操作类型，用于区分日志的操作类，目前支持的类型如下：

```java
public enum OperateTypeEnum{

    /**
     * 新增
     */
    INSERT,

    /**
     * 删除
     */
    DELETE,

    /**
     * 修改
     */
    UPDATE,

    /**
     * 查询
     */
    SELECT,

    ;
}
```

- com.lj.record.spring.boot.core.Operator 当前操作人，定义如下：

```java
public class Operator {

    /** id */
    private String id;

    /** 用户名 */
    private String name;
}
```

- com.lj.record.spring.boot.core.OperatorService 自定义当前操作人，定义如下：

```java
public interface OperatorService {

    /**
     * 获取操作者
     * @return 操作者
     */
    Operator getOperator();
}
```

- com.lj.record.spring.boot.core.Record 日志记录对象，保存日志数据的对象，定义如下：

```java
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
```

- com.lj.record.spring.boot.core.LogRecordService 自定义操作日志记录逻辑，定义如下：

```java
public interface LogRecordService {

    /**
     * 记录
     * @param record 日志记录
     */
    void record(Record record);
}
```

## 注意事项

本项目通过Spring AOP实现的日志记录，因此，需要避免Spring AOP不生效的情况。

常见的Spring AOP失效的情况有：

- Spring bean自调用

```java
@Service
public class UsService{
		
		public void updateUser(){
		  // 自调用会导致Spring AOP不生效
			updateUserStatus();
		}
		
		@LogRecord(describe ="#operator+'修改用户【'+#userName+'】状态由【'+#userStatus+'】改为【'+#status+'】'", operateType = OperateTypeEnum.UPDATE, bizNo="#userId")
		public void updateUserStatus(){
		
		}
}
```

- 错误的修饰符（非public、static、final）

```java
@Service
public class UsService{
		
		// Spring AOP只对public方法有效 其他权限修饰符会导致Spring AOP失效
		@LogRecord(describe ="#operator+'修改用户【'+#userName+'】状态由【'+#userStatus+'】改为【'+#status+'】'", operateType = OperateTypeEnum.UPDATE, bizNo="#userId")
		private void updateUserStatus(){
		
		}
		
		// static方法会导致Spring AOP失效
		@LogRecord(describe ="#operator+'修改用户【'+#userName+'】状态由【'+#userStatus+'】改为【'+#status+'】'", operateType = OperateTypeEnum.UPDATE, bizNo="#userId")
		public static void updateUserStatus(){
		
		}
		
		// final方法会导致Spring AOP失效
		@LogRecord(describe ="#operator+'修改用户【'+#userName+'】状态由【'+#userStatus+'】改为【'+#status+'】'", operateType = OperateTypeEnum.UPDATE, bizNo="#userId")
		public final void updateUserStatus(){
		
		}
}
```

- 非Spring bean调用Spring bean方法

```java
public class TestService{
		
		// 非Spring bean调用Spring bean的方法会导致Spring AOP失效
		public void updateUserStatus(){
			UserService userService = new UserService();
			userService.updateUserStatus();
		}
}
```

```java
@Service
public class UsService{

		@LogRecord(describe ="#operator+'修改用户【'+#userName+'】状态由【'+#userStatus+'】改为【'+#status+'】'", operateType = OperateTypeEnum.UPDATE, bizNo="#userId")
		public void updateUserStatus(){
		
		}
}
```

更多Spring AOP使用限制可参考[Spring AOP官方文档](https://docs.spring.io/spring-framework/reference/core/aop.html)。