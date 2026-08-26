package com.syncturtle.services.workspace.service;

public interface OutboxRelayService {
    void relayDueMessages(String workerId);
}
