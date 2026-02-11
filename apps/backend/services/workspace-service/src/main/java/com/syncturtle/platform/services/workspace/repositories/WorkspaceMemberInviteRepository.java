package com.syncturtle.platform.services.workspace.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.platform.services.workspace.models.WorkspaceMemberInvite;

public interface WorkspaceMemberInviteRepository extends JpaRepository<WorkspaceMemberInvite, UUID> {

}
