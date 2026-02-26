package com.example.resume_analyzer.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "resume_chunks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeChunk {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT")
    private String chunkText;

    // We will handle vector later using native query
//     @Column(columnDefinition = "vector")
// private String embedding;

    @PrePersist
    public void prePersist() {
        this.id = UUID.randomUUID();
    }
}