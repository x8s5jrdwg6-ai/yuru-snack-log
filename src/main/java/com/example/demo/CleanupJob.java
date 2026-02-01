package com.example.demo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CleanupJob {

    private final JdbcTemplate jdbc;

    public CleanupJob(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 毎週月曜日 4:00 に実行
    @Scheduled(cron = "0 0 4 ? * MON", zone = "Asia/Tokyo")
    public void deleteUnusedUser() {
    	String sql = """
    			DELETE FROM users
    			WHERE regist_date = last_access_date;
    			""";
        int deleted = jdbc.update(sql);

        System.out.println(
            "[CleanupJob] deleted rows = " + deleted
        );
    }
}
