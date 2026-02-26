package com.example.resume_analyzer.repository;

import com.example.resume_analyzer.domain.ResumeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ResumeChunkRepository extends JpaRepository<ResumeChunk, UUID> {

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO resume_chunks 
        (id, resume_id, chunk_index, chunk_text, embedding)
        VALUES (:id, :resumeId, :chunkIndex, :chunkText, CAST(:embedding AS vector))
        """, nativeQuery = true)
    void insertChunkWithEmbedding(
            @Param("id") UUID id,
            @Param("resumeId") UUID resumeId,
            @Param("chunkIndex") int chunkIndex,
            @Param("chunkText") String chunkText,
            @Param("embedding") String embedding
    );

    // ✅ return chunk_text + distance
   @Query(value = """
    SELECT chunk_text, (embedding <-> CAST(:queryVector AS vector)) AS distance
    FROM resume_chunks
    ORDER BY embedding <-> CAST(:queryVector AS vector)
    LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> searchSimilarChunksWithDistance(
            @Param("queryVector") String queryVector,
            @Param("limit") int limit
    );

     // Pull top chunks across ALL resumes, including resume_id and distance
   @Query(value = """
    SELECT resume_id, chunk_text, (embedding <-> CAST(:queryVector AS vector)) AS distance
    FROM resume_chunks
    ORDER BY embedding <-> CAST(:queryVector AS vector)
    LIMIT :topChunks
    """, nativeQuery = true)
    List<Object[]> topChunksAcrossAllResumes(
            @Param("queryVector") String queryVector,
            @Param("topChunks") int topChunks
    );

    @Query(value = """
    SELECT resume_id, chunk_text, (embedding <-> CAST(:queryVector AS vector)) AS distance
    FROM resume_chunks
    WHERE resume_id = :resumeId
    ORDER BY embedding <-> CAST(:queryVector AS vector)
    LIMIT :topK
    """, nativeQuery = true)
List<Object[]> topChunksForResume(
        @Param("resumeId") UUID resumeId,
        @Param("queryVector") String queryVector,
        @Param("topK") int topK
);
}