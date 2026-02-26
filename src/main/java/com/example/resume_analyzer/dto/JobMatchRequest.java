package com.example.resume_analyzer.dto;

import lombok.Data;

@Data
public class JobMatchRequest {
    private String jobDescription;
    private int limit = 5;        // how many resumes to return
    private int topChunks = 20;   // how many chunks to pull globally before grouping
}