package com.att.tdp.issueflow.repositories;

import com.att.tdp.issueflow.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByDeletedFalse();

    List<Project> findByDeletedTrue();

    Optional<Project> findByIdAndDeletedFalse(Long id);

    Optional<Project> findByIdAndDeletedTrue(Long id);

    boolean existsByIdAndDeletedFalse(Long id);
}