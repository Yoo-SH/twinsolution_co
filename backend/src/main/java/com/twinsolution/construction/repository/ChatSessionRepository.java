package com.twinsolution.construction.repository;

import com.twinsolution.construction.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByProjectIdOrderByUpdatedAtDesc(Long projectId);

    @Query("SELECT COUNT(cs) FROM ChatSession cs WHERE cs.createdAt >= :startDate")
    long countSessionsCreatedAfter(LocalDateTime startDate);
}
