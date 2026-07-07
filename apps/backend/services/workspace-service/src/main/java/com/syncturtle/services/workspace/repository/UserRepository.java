package com.syncturtle.services.workspace.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.workspace.model.UserLite;

public interface UserRepository extends JpaRepository<UserLite, UUID> {

}
