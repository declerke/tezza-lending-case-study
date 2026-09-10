package com.example.lending.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SweepJobScheduler {

    private final SweepJobService sweepJobService;

    public SweepJobScheduler(SweepJobService sweepJobService) {
        this.sweepJobService = sweepJobService;
    }

    @Scheduled(cron = "${lending.sweep-job.cron:0 0 1 * * *}")
    public void runScheduledSweep() {
        sweepJobService.runSweep();
    }
}
