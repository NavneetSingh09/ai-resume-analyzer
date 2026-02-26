package com.example.resume_analyzer.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AnalyzeResponse {

    private double overallScore;
    private String bestMatchingSection;

    private List<String> matchedSkills;
    private List<String> missingSkills;

    private String summary;
}