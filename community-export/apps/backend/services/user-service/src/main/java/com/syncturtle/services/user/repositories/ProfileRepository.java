package com.syncturtle.services.user.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.user.models.Profile;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

}
