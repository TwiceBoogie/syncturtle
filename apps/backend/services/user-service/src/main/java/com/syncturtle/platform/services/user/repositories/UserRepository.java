package com.syncturtle.platform.services.user.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.platform.services.user.models.User;

public interface UserRepository extends JpaRepository<User, UUID> {

}
