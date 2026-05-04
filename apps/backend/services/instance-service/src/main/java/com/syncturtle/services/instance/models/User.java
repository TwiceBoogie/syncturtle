package com.syncturtle.services.instance.models;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.data.jpa.support.ValidTimeZone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users_lite")
public class User {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "date_joined")
    private Instant dateJoined;

    @Column(name = "avatar_asset_id")
    private UUID avatarAssetId;

    @Column(name = "cover_image_asset_id")
    private UUID coverImageAssetId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "is_password_autoset", nullable = false)
    private boolean passwordAutoset;

    @ValidTimeZone
    @Column(name = "user_timezone")
    private String userTimezone;

    @Column(name = "is_bot", nullable = false)
    private boolean bot;

    @Column(name = "version", nullable = false)
    private Long version;
}
