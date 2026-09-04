package com.xmreader;

import com.xmreader.crawler.CrawlerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CrawlerProperties.class)
public class XmReaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(XmReaderApplication.class, args);
    }
}
