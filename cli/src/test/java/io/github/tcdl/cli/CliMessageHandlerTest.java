package io.github.tcdl.cli;

import org.junit.Test;

import java.util.Collections;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CliMessageHandlerTest {
    @Test
    public void testSubscriptionToResponseQueue() {
        CliMessageSubscriber subscriber = mock(CliMessageSubscriber.class);

        CliMessageHandler handler = new CliMessageHandler(subscriber, Collections.singletonList("response"), true);
        handler.onMessage(
                "{  \"topics\": {\n"
                        + "    \"to\": \"search:parsers:facets:v1\",\n"
                        + "    \"response\": \"search:parsers:facets:v1:response:3c3dec275b326c6500010843\"\n"
                        + "  }}"
        );

        verify(subscriber).subscribe("search:parsers:facets:v1:response:3c3dec275b326c6500010843", handler);
    }

    @Test
    public void testNoSubscriptionIfMissingResponseQueue() {
        CliMessageSubscriber subscriber = mock(CliMessageSubscriber.class);

        CliMessageHandler handler = new CliMessageHandler(subscriber, Collections.singletonList("response"), true);
        handler.onMessage(
                "{  \"topics\": {\n"
                        + "    \"to\": \"search:parsers:facets:v1\"\n"
                        + "  }}"
        );

        verifyNoMoreInteractions(subscriber);
    }

    @Test
    public void testNoSubscriptionIfNullResponseQueue() {
        CliMessageSubscriber subscriber = mock(CliMessageSubscriber.class);

        CliMessageHandler handler = new CliMessageHandler(subscriber, Collections.singletonList("response"), true);
        handler.onMessage(
                "{  \"topics\": {\n"
                        + "    \"to\": \"search:parsers:facets:v1\",\n"
                        + "    \"response\": null\n"
                        + "  }}"
        );

        verifyNoMoreInteractions(subscriber);
    }

    @Test
    public void testSubscriptionNonExistingQueue() {
        CliMessageSubscriber subscriber = mock(CliMessageSubscriber.class);
        CliMessageHandler handler = new CliMessageHandler(subscriber, Collections.singletonList("response"), true);

        doThrow(new RuntimeException()).when(subscriber).subscribe("non-existent-queue", handler);

        handler.onMessage(
                "{  \"topics\": {\n"
                        + "    \"to\": \"search:parsers:facets:v1\",\n"
                        + "    \"response\": \"non-existent-queue\"\n"
                        + "  }}"
        );

        // The point of this test is to verify that no exception is thrown in such case
        // that's why we don't have any explicit assert or verification here
    }
}