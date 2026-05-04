package com.syncturtle.services.workspace.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.workspace.models.WorkspaceMember;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

}
