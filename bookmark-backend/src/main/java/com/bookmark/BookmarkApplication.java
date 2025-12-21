package com.bookmark;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.bookmark.mapper")
@EnableScheduling
@EnableAsync
public class BookmarkApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookmarkApplication.class, args);
    }
}
