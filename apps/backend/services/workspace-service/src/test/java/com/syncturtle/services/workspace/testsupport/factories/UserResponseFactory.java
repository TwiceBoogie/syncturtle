// package com.syncturtle.services.workspace.testsupport.factories;

// import java.time.Instant;
// import java.util.UUID;

// import com.syncturtle.services.workspace.dto.response.UserResponse;

// public final class UserResponseFactory {

// private UserResponseFactory() {
// throw new AssertionError("No instance.");
// }

// public static UserResponse aUser() {
// UserResponse user = new UserResponse();
// user.setId(UUID.randomUUID());
// user.setUsername("lunasnow123");
// user.setEmail("lunasnow@marvel.com");
// user.setDisplayName("lunasnow");
// user.setFirstName("Luna");
// user.setLastName("Snow");
// user.setDateJoined(Instant.parse("2026-02-10T00:00:00Z"));
// user.setAvatarAssetId(null);
// user.setCoverImageAssetId(null);
// user.setActive(true);
// user.setEmailVerified(true);
// user.setPasswordAutoset(false);
// user.setTimezone("America/Chicago");
// user.setBot(false);
// return user;
// }

// public static UserResponse aUser(UUID id) {
// UserResponse user = aUser();
// user.setId(id);
// return user;
// }

// }
