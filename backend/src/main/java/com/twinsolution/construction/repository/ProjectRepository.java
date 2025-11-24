package com.twinsolution.construction.repository;

import com.twinsolution.construction.entity.Project;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    List<Project> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.status = :status")
    long countByStatus(String status);
}
