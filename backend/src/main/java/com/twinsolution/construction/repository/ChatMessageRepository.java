package com.twinsolution.construction.repository;

import com.twinsolution.construction.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAsc(Long chatSessionId);

    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAsc(Long chatSessionId, Pageable pageable);
}
