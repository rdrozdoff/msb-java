package io.github.tcdl;

import io.github.tcdl.config.MessageTemplate;
import io.github.tcdl.config.RequestOptions;
import io.github.tcdl.events.EventHandlers;
import io.github.tcdl.messages.Message;
import io.github.tcdl.messages.payload.Payload;
import io.github.tcdl.support.TestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by rdro on 4/27/2015.
 */
@RunWith(MockitoJUnitRunner.class)
public class RequesterTest {

    @Mock
    private EventHandlers eventHandlerMock;

    @Mock
    private ChannelManager channelManagerMock;

    @Mock
    private Producer producerMock;

    @Mock
    private Consumer consumerMock;

    @Mock
    private Collector collectorMock;

    @Test
    public void testPublishNoWaitForResponses() throws Exception {
        Requester requester = initRequesterForResponsesWithTimeout(0);

        requester.publish(TestUtils.createSimpleRequestPayload());

        verify(collectorMock, never()).listenForResponses(anyString(), any());
        verify(collectorMock, never()).waitForResponses();
    }

    @Test
    public void testPublishWaitForResponses() throws Exception {
        Requester requester = initRequesterForResponsesWithTimeout(1);

        requester.publish(TestUtils.createSimpleRequestPayload());

        verify(collectorMock).listenForResponses(anyString(), any(Message.class));
        verify(collectorMock).waitForResponses();
    }

    @Test
    public void testPublishCallProducerPublishWithPayload() throws Exception {
        Requester requester = initRequesterForResponsesWithTimeout(0);
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        Payload payload = TestUtils.createSimpleRequestPayload();

        requester.publish(payload);

        verify(producerMock).publish(messageCaptor.capture());
        assertThat(payload, is(messageCaptor.getValue().getPayload()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAcknowledgeEventHandlerIsAdded() throws Exception {
        Callback onAckMock = mock(Callback.class);
        Requester requester = initRequesterForResponsesWithTimeout(1);

        requester.onAcknowledge(onAckMock);

        assertThat(requester.eventHandlers.onAcknowledge(), is(onAckMock));
        assertThat(requester.eventHandlers.onResponse(), not(onAckMock));
        assertThat(requester.eventHandlers.onEnd(), not(onAckMock));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResponseEventHandlerIsAdded() throws Exception {
        Callback onResponseMock = mock(Callback.class);
        Requester requester = initRequesterForResponsesWithTimeout(1);

        requester.onResponse(onResponseMock);

        assertThat(requester.eventHandlers.onAcknowledge(), not(onResponseMock));
        assertThat(requester.eventHandlers.onResponse(), is(onResponseMock));
        assertThat(requester.eventHandlers.onEnd(), not(onResponseMock));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEndEventHandlerIsAdded() throws Exception {
        Callback onEndMock = mock(Callback.class);
        Requester requester = initRequesterForResponsesWithTimeout(1);

        requester.onEnd(onEndMock);

        assertThat(requester.eventHandlers.onAcknowledge(), not(onEndMock));
        assertThat(requester.eventHandlers.onResponse(), not(onEndMock));
        assertThat(requester.eventHandlers.onEnd(), is(onEndMock));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNoEventHandlerAdded() throws Exception {
        Callback onEndMock = mock(Callback.class);
        Requester requester = initRequesterForResponsesWithTimeout(1);

        assertThat(requester.eventHandlers.onAcknowledge(), not(onEndMock));
        assertThat(requester.eventHandlers.onResponse(), not(onEndMock));
        assertThat(requester.eventHandlers.onEnd(), not(onEndMock));
    }

    @Test
    public void testRequestMessage() throws Exception {
        Requester requester = initRequesterForResponsesWithTimeout(0);
        Payload request = TestUtils.createSimpleRequestPayload();

        requester.publish(request);

        Message requestMessage = requester.getMessage();
        assertNotNull(requestMessage);
        assertNotNull(requestMessage.getMeta());
        assertNotNull(requestMessage.getPayload());
    }

    private Requester initRequesterForResponsesWithTimeout(int numberOfResponses) throws Exception {
        RequestOptions requestOptionsMock = mock(RequestOptions.class);
        MessageTemplate messageTemplateMock = mock(MessageTemplate.class);

        when(requestOptionsMock.getMessageTemplate()).thenReturn(messageTemplateMock);
        when(requestOptionsMock.getWaitForResponses()).thenReturn(numberOfResponses);
        when(requestOptionsMock.isWaitForResponses()).thenReturn(numberOfResponses > 0 ? true : false);
        when(requestOptionsMock.getResponseTimeout()).thenReturn(100);
        when(channelManagerMock.findOrCreateProducer(anyString())).thenReturn(producerMock);

        MsbContext msbContext = TestUtils.createSimpleMsbContext();
        msbContext = TestUtils.createMsbContext(msbContext.getMsbConfig(), msbContext.getMessageFactory(), 
                channelManagerMock, msbContext.getClock(), msbContext.getTimeoutManager());

        Requester requester = spy(Requester.create("test:requester", requestOptionsMock, null, msbContext));

        collectorMock = spy(new Collector(requestOptionsMock, msbContext, eventHandlerMock));

        doReturn(collectorMock)
                .when(requester)
                .createCollector(any(RequestOptions.class), any(MsbContext.class), any(EventHandlers.class));

        return requester;
    }

}
