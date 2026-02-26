package com.example.resume_analyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchResult {
    private String chunkText;
    private double distance;    // smaller = more similar
    private double score;       // 0-100 (higher = better)
}