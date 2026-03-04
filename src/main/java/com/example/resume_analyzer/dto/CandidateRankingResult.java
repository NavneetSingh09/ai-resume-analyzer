package com.example.resume_analyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CandidateRankingResult {

    private UUID resumeId;
    private double score;
    private String bestMatchingChunk;

}