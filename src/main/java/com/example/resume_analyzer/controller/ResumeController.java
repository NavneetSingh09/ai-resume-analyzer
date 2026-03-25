package com.example.resume_analyzer.controller;

import com.example.resume_analyzer.domain.Resume;
import com.example.resume_analyzer.dto.*;
import com.example.resume_analyzer.service.PdfService;
import com.example.resume_analyzer.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final PdfService pdfService;

    @GetMapping
    public List<Resume> getAll() {
        return resumeService.getAllResumes();
    }

    @PostMapping
    public Resume save(@RequestBody Resume request) {
        return resumeService.saveResume(request.getContent());
    }

    @PostMapping("/upload")
    public Resume uploadResume(@RequestParam("file") MultipartFile file) {
        String text = pdfService.extractText(file);
        return resumeService.saveResume(text);
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

    @PostMapping("/analyze-rag")
    public String analyzeRAG(@RequestBody JobMatchRequest request) {
        return resumeService.analyzeWithRAG(
                request.getJobDescription(),
                request.getTopChunks()
        );
    }

    @PostMapping("/jobs/rank")
    public List<CandidateRankingResult> rankCandidates(
            @RequestBody JobRankingRequest request
    ) {
        return resumeService.rankCandidates(
                request.getJobDescription(),
                request.getTopCandidates()
        );
    }
}