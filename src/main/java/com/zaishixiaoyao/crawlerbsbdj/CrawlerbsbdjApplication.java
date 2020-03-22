package com.zaishixiaoyao.crawlerbsbdj;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication

@MapperScan("com.zaishixiaoyao.crawlerbsbdj.mapper")//扫描mapper接口的包，自动生成mapper对应的实现类
@EnableScheduling //EnableScheduling启用任务调度
public class CrawlerbsbdjApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(CrawlerbsbdjApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(CrawlerbsbdjApplication.class);
    }

}
