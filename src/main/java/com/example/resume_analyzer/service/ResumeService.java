package com.example.resume_analyzer.service;

import com.example.resume_analyzer.domain.Resume;
import com.example.resume_analyzer.repository.ResumeChunkRepository;
import com.example.resume_analyzer.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.resume_analyzer.dto.SearchResult;
import com.example.resume_analyzer.dto.AnalyzeRequest;
import com.example.resume_analyzer.dto.AnalyzeResponse;
import com.example.resume_analyzer.dto.CandidateRankingResult;
import com.example.resume_analyzer.dto.JobMatchResult;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final OpenAIChatService openAIChatService;

    public Resume saveResume(String content) {

        Resume resume = new Resume();
        resume.setContent(content);
        Resume savedResume = resumeRepository.save(resume);

        List<String> chunks = splitIntoChunks(content, 500, 100);

        int index = 0;
        for (String chunkText : chunks) {

            List<Double> embedding = embeddingService.getEmbedding(chunkText);
            if (embedding == null || embedding.isEmpty()) {
                continue;
            }

            String vectorString = "[" +
                    embedding.stream()
                            .map(d -> String.format(Locale.US, "%.10f", d))
                            .collect(Collectors.joining(",")) +
                    "]";

            chunkRepository.insertChunkWithEmbedding(
                    UUID.randomUUID(),
                    savedResume.getId(),
                    index++,
                    chunkText,
                    vectorString
            );
        }

        return savedResume;
    }

    public List<Resume> getAllResumes() {
        return resumeRepository.findAll();
    }

    private List<String> splitIntoChunks(String text, int maxChunkSize, int overlap) {

        String[] sentences = text.split("(?<=[.!?\\n])\\s+");

        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        List<String> currentSentences = new ArrayList<>();

        for (String sentence : sentences) {

            if (currentChunk.length() + sentence.length() > maxChunkSize
                    && !currentChunk.isEmpty()) {

                chunks.add(currentChunk.toString().trim());

                StringBuilder overlapBuilder = new StringBuilder();
                List<String> overlapSentences = new ArrayList<>();

                for (int i = currentSentences.size() - 1; i >= 0; i--) {
                    String s = currentSentences.get(i);
                    if (overlapBuilder.length() + s.length() > overlap) break;
                    overlapSentences.add(0, s);
                    overlapBuilder.insert(0, s + " ");
                }

                currentChunk = new StringBuilder(overlapBuilder.toString());
                currentSentences = new ArrayList<>(overlapSentences);
            }

            currentChunk.append(sentence).append(" ");
            currentSentences.add(sentence);
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    public List<SearchResult> semanticSearch(String query, int limit) {

        List<Double> embedding = embeddingService.getEmbedding(query);
        if (embedding == null || embedding.isEmpty()) {
            return List.of();
        }

        String vectorString = toVectorString(embedding);

        List<Object[]> rows = chunkRepository.searchSimilarChunksWithDistance(vectorString, limit);

        List<SearchResult> results = new ArrayList<>();
        for (Object[] row : rows) {
            String chunkText = (String) row[0];
            double distance = ((Number) row[1]).doubleValue();
            double score = distanceToScore(distance);
            results.add(new SearchResult(chunkText, distance, round2(score)));
        }

        return results;
    }

    public List<JobMatchResult> matchResumesToJob(String jobDescription, int limit, int topChunksPerResume) {

        List<Double> embedding = embeddingService.getEmbedding(jobDescription);
        if (embedding == null || embedding.isEmpty()) return List.of();

        String queryVector = toVectorString(embedding);

        int candidatePoolSize = Math.max(50, limit * 10);
        List<Object[]> globalRows = chunkRepository.topChunksAcrossAllResumes(queryVector, candidatePoolSize);

        if (globalRows == null || globalRows.isEmpty()) return List.of();

        Set<UUID> resumeIds = new LinkedHashSet<>();
        for (Object[] r : globalRows) {
            resumeIds.add((UUID) r[0]);
        }

        List<JobMatchResult> results = new ArrayList<>();

        for (UUID resumeId : resumeIds) {

            List<Object[]> topChunks =
                    chunkRepository.topChunksForResume(resumeId, queryVector, topChunksPerResume);

            if (topChunks.isEmpty()) continue;

            String bestChunk = (String) topChunks.get(0)[1];
            double bestDistance = ((Number) topChunks.get(0)[2]).doubleValue();

            double bestScore = distanceToScore(bestDistance);

            double avgTopScore = topChunks.stream()
                    .mapToDouble(x -> distanceToScore(((Number) x[2]).doubleValue()))
                    .average()
                    .orElse(0.0);

            double finalScore = (0.7 * bestScore) + (0.3 * avgTopScore);

            results.add(new JobMatchResult(
                    resumeId,
                    bestChunk,
                    round2(bestDistance),
                    round2(finalScore)
            ));
        }

        results.sort(Comparator.comparingDouble(JobMatchResult::getScore).reversed());

        return results.stream().limit(limit).toList();
    }

    public AnalyzeResponse analyzeResume(AnalyzeRequest request) {

        Resume saved = saveResume(request.getResumeText());

        List<JobMatchResult> matchResults =
                matchResumesToJob(request.getJobDescription(), 1, 3);

        if (matchResults.isEmpty()) {
            return AnalyzeResponse.builder()
                    .overallScore(0)
                    .summary("No match found.")
                    .build();
        }

        JobMatchResult topMatch = matchResults.get(0);

        List<String> resumeSkills = extractSkills(request.getResumeText());
        List<String> jobSkills = extractSkills(request.getJobDescription());

        List<String> matched = resumeSkills.stream()
                .filter(jobSkills::contains)
                .toList();

        List<String> missing = jobSkills.stream()
                .filter(skill -> !resumeSkills.contains(skill))
                .toList();

        String summary = "Strong alignment in " + matched +
                ". Missing skills: " + missing;

        return AnalyzeResponse.builder()
                .overallScore(topMatch.getScore())
                .bestMatchingSection(topMatch.getBestMatchingChunk())
                .matchedSkills(matched)
                .missingSkills(missing)
                .summary(summary)
                .build();
    }

    public String analyzeWithRAG(String jobDescription, int topChunks) {

        List<Double> embedding = embeddingService.getEmbedding(jobDescription);
        if (embedding == null || embedding.isEmpty()) {
            return "Embedding failed";
        }

        String queryVector = toVectorString(embedding);

        List<Object[]> rows =
                chunkRepository.topChunksAcrossAllResumes(queryVector, topChunks);

        StringBuilder context = new StringBuilder();
        for (Object[] r : rows) {
            context.append("- ").append((String) r[1]).append("\n");
        }

        String prompt = """
                Based on the following resume sections:

                %s

                Evaluate how well this resume matches the job description:

                %s

                Provide:
                - Overall score (0-100)
                - Strengths
                - Missing skills
                - Improvement suggestions
                """.formatted(context.toString(), jobDescription);

        return openAIChatService.generateAnalysis(prompt);
    }

    public List<CandidateRankingResult> rankCandidates(String jobDescription, int topCandidates) {

        List<Double> embedding = embeddingService.getEmbedding(jobDescription);
        if (embedding == null || embedding.isEmpty()) return List.of();

        String queryVector = toVectorString(embedding);

        List<Object[]> rows = chunkRepository.rankCandidates(queryVector, topCandidates);

        List<CandidateRankingResult> results = new ArrayList<>();

        for (Object[] row : rows) {
            UUID resumeId = (UUID) row[0];
            String chunk = (String) row[1];
            double distance = ((Number) row[2]).doubleValue();
            double score = distanceToScore(distance);
            results.add(new CandidateRankingResult(resumeId, round2(score), chunk));
        }

        return results;
    }

    // ── Utility methods ──

    private String toVectorString(List<Double> embedding) {
        return "[" +
                embedding.stream()
                        .map(d -> String.format(Locale.US, "%.10f", d))
                        .collect(Collectors.joining(",")) +
                "]";
    }

    private double distanceToScore(double distance) {
        double alpha = 2.0;
        return 100.0 * Math.exp(-alpha * distance);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private List<String> extractSkills(String text) {

        List<String> knownSkills = List.of(
                "Java", "Spring Boot", "PostgreSQL",
                "React", "Docker", "Kubernetes",
                "Python", "AWS", "Microservices",
                "Machine Learning", "TypeScript", "Node.js",
                "GraphQL", "Redis", "Kafka",
                "CI/CD", "Terraform", "Go",
                "MongoDB", "REST API", "gRPC",
                "Git", "Linux", "SQL",
                "NoSQL", "RabbitMQ", "Elasticsearch",
                "Angular", "Vue.js", "Swift",
                "Kotlin", "C++", "Rust",
                "Azure", "GCP", "Jenkins",
                "Spark", "Hadoop", "Airflow",
                "TensorFlow", "PyTorch", "NLP",
                "Computer Vision", "Deep Learning",
                "Data Engineering", "ETL"
        );

        List<String> found = new ArrayList<>();
        String lowerText = text.toLowerCase();

        for (String skill : knownSkills) {
            if (lowerText.contains(skill.toLowerCase())) {
                found.add(skill);
            }
        }

        return found;
    }
}