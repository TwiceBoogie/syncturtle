package com.syncturtle.services.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.common.contracts.auth.provider.AuthProvider;
import com.syncturtle.services.user.model.Account;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByProviderAndProviderAccountId(AuthProvider provider, String providerAccountId);
}
