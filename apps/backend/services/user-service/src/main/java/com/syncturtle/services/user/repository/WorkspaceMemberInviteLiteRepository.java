package com.syncturtle.services.user.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.user.model.WorkspaceMemberInviteLite;

public interface WorkspaceMemberInviteLiteRepository extends JpaRepository<WorkspaceMemberInviteLite, UUID> {
    long countByEmailAndAcceptedFalseAndDeletedAtIsNull(String email);
}
