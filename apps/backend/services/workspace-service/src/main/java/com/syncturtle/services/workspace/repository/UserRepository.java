package com.syncturtle.services.workspace.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.workspace.model.User;

public interface UserRepository extends JpaRepository<User, UUID> {

}
