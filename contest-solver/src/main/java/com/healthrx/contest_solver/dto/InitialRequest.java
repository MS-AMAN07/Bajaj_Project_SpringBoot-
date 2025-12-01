package com.healthrx.contest_solver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitialRequest {
    private String name;
    private String regNo;
    private String email;
}