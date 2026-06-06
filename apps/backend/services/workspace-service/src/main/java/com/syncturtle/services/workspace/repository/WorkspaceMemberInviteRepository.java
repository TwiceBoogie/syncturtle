package com.syncturtle.services.workspace.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.workspace.model.WorkspaceMemberInvite;

public interface WorkspaceMemberInviteRepository extends JpaRepository<WorkspaceMemberInvite, UUID> {

}
