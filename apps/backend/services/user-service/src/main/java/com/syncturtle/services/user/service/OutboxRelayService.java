package com.syncturtle.services.user.service;

public interface OutboxRelayService {
    void relayDueMessages(String workerId);
}
