package com.example.resume_analyzer.controller;

import com.example.resume_analyzer.domain.Resume;
import com.example.resume_analyzer.dto.SearchRequest;
import com.example.resume_analyzer.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.example.resume_analyzer.dto.SearchResult;
import com.example.resume_analyzer.dto.AnalyzeRequest;
import com.example.resume_analyzer.dto.AnalyzeResponse;
import com.example.resume_analyzer.dto.JobMatchRequest;
import com.example.resume_analyzer.dto.JobMatchResult;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    public List<Resume> getAll() {
        return resumeService.getAllResumes();
    }

    @PostMapping
    public Resume save(@RequestBody Resume request) {
        return resumeService.saveResume(request.getContent());
    }

   @PostMapping("/search")
public List<SearchResult> search(@RequestBody SearchRequest request) {
    return resumeService.semanticSearch(request.getQuery(), 5);
}
@PostMapping("/match")
public List<JobMatchResult> match(@RequestBody JobMatchRequest request) {
    return resumeService.matchResumesToJob(
            request.getJobDescription(),
            request.getLimit(),
            request.getTopChunks()
    );
}

@PostMapping("/analyze")
public AnalyzeResponse analyze(@RequestBody AnalyzeRequest request) {
    return resumeService.analyzeResume(request);
}
}