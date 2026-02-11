package com.syncturtle.platform.services.workspace.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.platform.services.workspace.models.User;

public interface UserRepository extends JpaRepository<User, UUID> {

}
