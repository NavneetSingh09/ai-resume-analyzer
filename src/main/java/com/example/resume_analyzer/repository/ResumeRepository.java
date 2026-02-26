package com.example.resume_analyzer.repository;

import com.example.resume_analyzer.domain.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {
}
