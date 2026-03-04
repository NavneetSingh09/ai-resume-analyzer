package com.example.resume_analyzer.dto;

import lombok.Data;

@Data
public class JobRankingRequest {

    private String jobDescription;
    private int topCandidates;

}