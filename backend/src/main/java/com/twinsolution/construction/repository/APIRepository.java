package com.twinsolution.construction.repository;

import com.twinsolution.construction.entity.API;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface APIRepository extends JpaRepository<API, Long> {

    List<API> findByStatus(String status);

    List<API> findAllByOrderByCreatedAtDesc();
}
