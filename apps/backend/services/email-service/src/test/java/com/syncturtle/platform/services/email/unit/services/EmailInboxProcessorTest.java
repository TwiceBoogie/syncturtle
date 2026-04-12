package com.syncturtle.platform.services.email.unit.services;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syncturtle.platform.services.email.service.EmailDispatchService;
import com.syncturtle.platform.services.email.service.EmailEventInboxService;
import com.syncturtle.platform.services.email.service.EmailInboxProcessor;

@ExtendWith(MockitoExtension.class)
class EmailInboxProcessorTest {

    @Mock
    EmailDispatchService emailDispatchService;

    @Mock
    EmailEventInboxService emailEventInboxService;

    @InjectMocks
    EmailInboxProcessor processor;

}
