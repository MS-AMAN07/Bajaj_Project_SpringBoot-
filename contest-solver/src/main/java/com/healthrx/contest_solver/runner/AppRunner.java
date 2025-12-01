
package com.healthrx.contest_solver.runner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.healthrx.contest_solver.service.ContestService;

@Component
public class AppRunner implements CommandLineRunner {

    private final ContestService contestService;

    public AppRunner(ContestService contestService) {
        this.contestService = contestService;
    }

    @Override
    public void run(String... args) throws Exception {
       
        contestService.executeContestFlow();
        

    }
}