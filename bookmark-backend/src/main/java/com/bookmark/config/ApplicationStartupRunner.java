package com.bookmark.config;

import com.bookmark.service.ActivationExpiryTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时的初始化操作
 * 同步现有激活码到 Redis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStartupRunner implements CommandLineRunner {

    private final ActivationExpiryTask activationExpiryTask;

    @Override
    public void run(String... args) throws Exception {
        log.info("应用启动完成，开始执行初始化操作...");

        try {
            // 同步现有激活码到 Redis
            activationExpiryTask.syncActiveActivationsToRedis();
        } catch (Exception e) {
            log.error("应用启动初始化失败", e);
        }

        log.info("应用初始化完成");
    }
}
