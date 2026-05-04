package com.syncturtle.services.workspace.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.workspace.models.WorkspaceMemberInvite;

public interface WorkspaceMemberInviteRepository extends JpaRepository<WorkspaceMemberInvite, UUID> {

}
