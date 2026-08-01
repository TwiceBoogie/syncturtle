package com.syncturtle.services.workspace.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.workspace.model.Workspace;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    boolean existsBySlugIgnoreCaseAndDeletedAtIsNull(String slug);

    Optional<Workspace> findBySlugAndDeletedAtIsNull(String slug);
}
