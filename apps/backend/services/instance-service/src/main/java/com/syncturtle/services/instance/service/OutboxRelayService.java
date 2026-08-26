package com.syncturtle.services.instance.service;

public interface OutboxRelayService {
    void relayDueMessages(String workerId);
}
