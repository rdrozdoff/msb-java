package io.github.tcdl.acceptance;

import io.github.tcdl.api.Requester;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by rdrozdov-tc on 6/15/15.
 */
public class RequesterResponderTest {

    private static final Integer NUMBER_OF_RESPONSES = 1;

    final String NAMESPACE = "test:requester-responder-example";

    private MsbTestHelper helper = MsbTestHelper.getInstance();

    private CountDownLatch passedLatch;

    public boolean isPassed() {
        try {
            passedLatch.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return false;
        }

        return passedLatch.getCount() == 0;
    }

    public void runRequesterResponder() throws Exception {
        helper.initDefault();
        // running responder server
        helper.createResponderServer(NAMESPACE, (request, responder) -> {
            responder.sendAck(1000, NUMBER_OF_RESPONSES);
            helper.respond(responder);
        })
        .listen();

        // sending a request
        Requester requester = helper.createRequester(NAMESPACE, NUMBER_OF_RESPONSES);
        passedLatch = new CountDownLatch(1);
        helper.sendRequest(requester, NUMBER_OF_RESPONSES, payload -> passedLatch.countDown());
    }
}