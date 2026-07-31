package com.syncturtle.services.user.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.user.model.WorkspaceLite;

public interface WorkspaceLiteRepository extends JpaRepository<WorkspaceLite, UUID> {

}
