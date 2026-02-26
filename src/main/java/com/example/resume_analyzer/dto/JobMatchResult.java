package com.example.resume_analyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class JobMatchResult {
    private UUID resumeId;
    private String bestMatchingChunk;
    private double distance;
    private double score;
}