package org.sdb.buzzfeed;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.sdb.buzzfeed.mapper")
public class BuzzFeedApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuzzFeedApplication.class, args);
    }

}
