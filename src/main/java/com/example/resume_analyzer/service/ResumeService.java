package com.example.resume_analyzer.service;

import com.example.resume_analyzer.domain.Resume;
import com.example.resume_analyzer.repository.ResumeChunkRepository;
import com.example.resume_analyzer.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.example.resume_analyzer.dto.SearchResult;
import com.example.resume_analyzer.dto.AnalyzeRequest;
import com.example.resume_analyzer.dto.AnalyzeResponse;
import com.example.resume_analyzer.dto.JobMatchResult;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;   // ✅ NEW

    public Resume saveResume(String content) {

        Resume resume = new Resume();
        resume.setContent(content);

        Resume savedResume = resumeRepository.save(resume);

        List<String> chunks = splitIntoChunks(content, 200);

        int index = 0;

        for (String chunkText : chunks) {

            List<Double> embedding = embeddingService.getEmbedding(chunkText);

            if (embedding == null || embedding.isEmpty()) {
                continue;
            }

            // Convert to proper decimal vector format
            String vectorString = "[" +
                    embedding.stream()
                            .map(d -> String.format("%.10f", d))
                            .reduce((a, b) -> a + "," + b)
                            .orElse("") +
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

    private List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();

        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }

        return chunks;
    }

    // 🔥 FINAL FIXED SEARCH METHOD
   public List<SearchResult> semanticSearch(String query, int limit) {

    List<Double> embedding = embeddingService.getEmbedding(query);
    if (embedding == null || embedding.isEmpty()) {
        return List.of();
    }

    // vector format: [0.123,-0.456,...]
    String vectorString = "[" +
            embedding.stream()
                    .map(d -> String.format(java.util.Locale.US, "%.10f", d))
                    .reduce((a, b) -> a + "," + b)
                    .orElse("") +
            "]";

    List<Object[]> rows = chunkRepository.searchSimilarChunksWithDistance(vectorString, limit);

    List<SearchResult> results = new ArrayList<>();
    for (Object[] row : rows) {
        String chunkText = (String) row[0];

        // row[1] can come as Double or BigDecimal depending on driver
        double distance = ((Number) row[1]).doubleValue();

        // cosine distance: 0 best, 1 worst(ish)
        double score = Math.max(0.0, (1.0 - distance) * 100.0);

        results.add(new SearchResult(chunkText, distance, score));
    }

    return results;
}

public List<JobMatchResult> matchResumesToJob(String jobDescription, int limit, int topChunksPerResume) {

    // 1) Embed job description
    List<Double> embedding = embeddingService.getEmbedding(jobDescription);
    if (embedding == null || embedding.isEmpty()) return List.of();

    String queryVector = "[" +
            embedding.stream()
                    .map(d -> String.format(Locale.US, "%.10f", d))
                    .collect(Collectors.joining(",")) +
            "]";

    // 2) Pull candidate chunks globally (bigger pool)
    int candidatePoolSize = Math.max(50, limit * 10);
    List<Object[]> globalRows = chunkRepository.topChunksAcrossAllResumes(queryVector, candidatePoolSize);

    if (globalRows == null || globalRows.isEmpty()) return List.of();

    // 3) Collect unique resume IDs
    Set<UUID> resumeIds = new LinkedHashSet<>();
    for (Object[] r : globalRows) {
        resumeIds.add((UUID) r[0]);
    }

    List<JobMatchResult> results = new ArrayList<>();

    // 4) For each resume → get top K chunks and compute weighted score
    for (UUID resumeId : resumeIds) {

        List<Object[]> topChunks =
                chunkRepository.topChunksForResume(resumeId, queryVector, topChunksPerResume);

        if (topChunks.isEmpty()) continue;

        // Best chunk
        String bestChunk = (String) topChunks.get(0)[1];
        double bestDistance = ((Number) topChunks.get(0)[2]).doubleValue();

        double bestScore = distanceToScore(bestDistance);

        // Average score of top K chunks
        double avgTopScore = topChunks.stream()
                .mapToDouble(x -> distanceToScore(((Number) x[2]).doubleValue()))
                .average()
                .orElse(0.0);

        // Weighted final score (70% best, 30% avg)
        double finalScore = (0.7 * bestScore) + (0.3 * avgTopScore);

        results.add(new JobMatchResult(
                resumeId,
                bestChunk,
                round2(bestDistance),
                round2(finalScore)
        ));
    }

    // 5) Sort descending
    results.sort(Comparator.comparingDouble(JobMatchResult::getScore).reversed());

    return results.stream().limit(limit).toList();
}

private double distanceToScore(double distance) {
    // Stable scoring function
    double alpha = 2.0; // increase to penalize distance more
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
            "Machine Learning"
    );

    List<String> found = new ArrayList<>();

    for (String skill : knownSkills) {
        if (text.toLowerCase().contains(skill.toLowerCase())) {
            found.add(skill);
        }
    }

    return found;
}

public AnalyzeResponse analyzeResume(AnalyzeRequest request) {

    // 1️⃣ Save resume to DB (generate chunks)
    Resume saved = saveResume(request.getResumeText());

    // 2️⃣ Run matcher against job
    List<JobMatchResult> matchResults =
            matchResumesToJob(request.getJobDescription(), 1, 3);

    if (matchResults.isEmpty()) {
        return AnalyzeResponse.builder()
                .overallScore(0)
                .summary("No match found.")
                .build();
    }

    JobMatchResult topMatch = matchResults.get(0);

    // 3️⃣ Extract skills
    List<String> resumeSkills = extractSkills(request.getResumeText());
    List<String> jobSkills = extractSkills(request.getJobDescription());

    // 4️⃣ Compute matched & missing skills
    List<String> matched = resumeSkills.stream()
            .filter(jobSkills::contains)
            .toList();

    List<String> missing = jobSkills.stream()
            .filter(skill -> !resumeSkills.contains(skill))
            .toList();

    // 5️⃣ Build summary
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
private final OpenAIChatService openAIChatService;
public String analyzeWithRAG(String jobDescription, int topChunks) {

    List<Double> embedding = embeddingService.getEmbedding(jobDescription);
    if (embedding == null || embedding.isEmpty()) {
        return "Embedding failed";
    }

    String queryVector = "[" +
            embedding.stream()
                    .map(d -> String.format(java.util.Locale.US, "%.10f", d))
                    .collect(Collectors.joining(",")) +
            "]";

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

}