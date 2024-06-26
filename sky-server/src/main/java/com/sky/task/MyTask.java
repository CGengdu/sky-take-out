package com.sky.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自定义的定时任务类
 */
@Component
@Slf4j
public class MyTask {
    //@Scheduled(cron = "0/5 * * * * ?")
    public void executeTask() {
        log.info("MyTask executeTask: {}", LocalDateTime.now());
    }
}
