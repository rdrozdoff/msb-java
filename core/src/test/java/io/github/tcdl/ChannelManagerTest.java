package io.github.tcdl;

import io.github.tcdl.config.MsbConfigurations;
import io.github.tcdl.messages.Message;
import io.github.tcdl.monitor.ChannelMonitorAgent;
import io.github.tcdl.support.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.ws.Holder;
import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author rdro
 * @since 4/24/2015
 */
public class ChannelManagerTest {

    private ChannelManager channelManager;
    private ChannelMonitorAgent mockChannelMonitorAgent;
    private Consumer.Subscriber subscriberMock;

    @Before
    public void setUp() {
        MsbConfigurations msbConfig = TestUtils.createMsbConfigurations();
        Clock clock = Clock.systemDefaultZone();
        this.channelManager = new ChannelManager(msbConfig, clock);

        mockChannelMonitorAgent = mock(ChannelMonitorAgent.class);
        channelManager.setChannelMonitorAgent(mockChannelMonitorAgent);
        subscriberMock = mock(Consumer.Subscriber.class);
    }

    @Test
    public void testProducerCached() {
        String topic = "topic:test";

        // Producer was created and monitor agent notified
        Producer producer1 = channelManager.findOrCreateProducer(topic);
        assertNotNull(producer1);
        verify(mockChannelMonitorAgent).producerTopicCreated(topic);

        // Cached producer was removed and monitor agent wasn't notified
        Producer producer2 = channelManager.findOrCreateProducer(topic);
        assertNotNull(producer2);
        assertSame(producer1, producer2);
        verifyNoMoreInteractions(mockChannelMonitorAgent);
    }

    @Test
    public void testConsumerCached() throws Exception {
        String topic = "topic:test";

        channelManager.subscribe(topic, subscriberMock);
        verify(mockChannelMonitorAgent).consumerTopicCreated(topic);

        channelManager.subscribe(topic, subscriberMock);
        verifyNoMoreInteractions(mockChannelMonitorAgent);
    }

    @Test
    public void testUnsubscribe() {
        String topic = "topic:test";

        channelManager.unsubscribe(topic, subscriberMock);
        verify(mockChannelMonitorAgent, never()).consumerTopicRemoved(topic);

        channelManager.subscribe(topic, subscriberMock); // force creation of the consumer
        channelManager.unsubscribe(topic, subscriberMock);
        verify(mockChannelMonitorAgent).consumerTopicRemoved(topic);
    }

    @Test
    public void testPublishMessageInvokesAgent() {
        String topic = "topic:test";

        Producer producer = channelManager.findOrCreateProducer(topic);
        Message message = TestUtils.createMsbRequestMessageWithPayloadAndTopicTo(topic);
        producer.publish(message);

        verify(mockChannelMonitorAgent).producerMessageSent(topic);
    }

    @Test
    public void testReceiveMessageInvokesAgentAndEmitsEvent() throws InterruptedException {
        String topic = "topic:test";

        CountDownLatch awaitReceiveEvents = new CountDownLatch(1);
        final Holder<Message> messageEvent = new Holder<>();

        Message message = TestUtils.createMsbRequestMessageWithPayloadAndTopicTo(topic);
        channelManager.findOrCreateProducer(topic).publish(message);
        channelManager.subscribe(topic, (msg, exception) -> {
            messageEvent.value = msg;
            awaitReceiveEvents.countDown();
        });

        assertTrue(awaitReceiveEvents.await(3000, TimeUnit.MILLISECONDS));
        verify(mockChannelMonitorAgent).consumerMessageReceived(topic);
        assertNotNull(messageEvent.value);
    }
}