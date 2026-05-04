package com.syncturtle.services.user.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.user.models.Account;

public interface AccountRepository extends JpaRepository<Account, UUID> {

}
