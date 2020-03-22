package com.zaishixiaoyao.crawlerbsbdj.common.task;

import com.zaishixiaoyao.crawlerbsbdj.service.CrawlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component //会被IOC容器加载
public class CrawlerTask {
    Logger logger = LoggerFactory.getLogger(CrawlerTask.class);//日志输出

    //在程序运行时自动会将参数注入到urls变量中
    @Value("${app.crawler.enabled}")
    private Boolean enabled; //开启关闭爬虫

    @Resource
    private CrawlerService crawlerService ;

    //任务执行的方法
    @Scheduled(cron = "${app.crawler.cron}")//每小时执行一次（测试时设定每分钟执行一次）
    public void crawlerTask(){

        if(enabled == false){
            logger.warn("爬虫任务已被禁止，如需启用请设置app.crawler.enabled=true");
            return;
        }

        // 1、URL模板：抓取原始数据
        crawlerService.crawlRunner();

        //2、对Source表数据进行处理
        crawlerService.etl();
    }
}
