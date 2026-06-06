package com.syncturtle.services.user.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.user.model.Profile;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

}
