package com.example.lending.controller;

import com.example.lending.scheduler.SweepJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SweepJobService sweepJobService;

    public AdminController(SweepJobService sweepJobService) {
        this.sweepJobService = sweepJobService;
    }

    @PostMapping("/sweep-job/run")
    public ResponseEntity<Void> runSweepJob() {
        sweepJobService.runSweep();
        return ResponseEntity.ok().build();
    }
}
