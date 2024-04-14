package io.github.lvlaotou.log.record.log.record.spring.boot.core;

/**
 * 日志记录业务接口
 * @author lvlaotou
 */
public interface LogRecordService {

    /**
     * 记录
     * @param record 日志记录
     */
    void record(Record record);
}
