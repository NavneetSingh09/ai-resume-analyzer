package com.example.resume_analyzer.dto;

import lombok.Data;

@Data
public class AnalyzeRequest {
    private String resumeText;
    private String jobDescription;
}