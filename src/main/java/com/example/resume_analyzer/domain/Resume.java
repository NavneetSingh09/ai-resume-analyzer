package com.example.resume_analyzer.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resumes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resume {

    @Id
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
    }
}
