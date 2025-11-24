package com.twinsolution.construction.repository;

import com.twinsolution.construction.entity.APILog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface APILogRepository extends JpaRepository<APILog, Long> {

    List<APILog> findByApiIdOrderByCreatedAtDesc(Long apiId, Pageable pageable);

    List<APILog> findByApiIdAndStatusOrderByCreatedAtDesc(Long apiId, String status, Pageable pageable);
}
