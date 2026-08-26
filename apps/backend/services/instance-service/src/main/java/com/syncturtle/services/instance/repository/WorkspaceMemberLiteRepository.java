package com.syncturtle.services.instance.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.instance.model.WorkspaceMemberLite;

public interface WorkspaceMemberLiteRepository extends JpaRepository<WorkspaceMemberLite, UUID> {

}
