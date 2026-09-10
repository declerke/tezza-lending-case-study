package com.example.lending.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lending")
public record LendingProperties(SweepJob sweepJob, Notifications notifications) {
    public LendingProperties {
        if (sweepJob == null) {
            sweepJob = new SweepJob("0 0 1 * * *");
        }
        if (notifications == null) {
            notifications = new Notifications(1);
        }
    }

    public record SweepJob(String cron) {}

    public record Notifications(int dueSoonReminderDays) {}
}
