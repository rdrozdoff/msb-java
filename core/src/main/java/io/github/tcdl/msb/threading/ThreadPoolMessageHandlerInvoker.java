package io.github.tcdl.msb.threading;

import io.github.tcdl.msb.api.message.Message;
import io.github.tcdl.msb.support.Utils;

import java.util.concurrent.ExecutorService;

/**
 * Concurrent {@link MessageHandlerInvoker} implementation used to invoke all {@link io.github.tcdl.msb.MessageHandler}
 * in a single thread pool with a configured number of threads. This approach is effective, but may lead
 * to concurrent issues when incoming messages order matters. When facing this kind of issues,
 * it is possible either to configure this class to work in a single-threaded mode,
 * or use {@link GroupedMessageHandlerInvoker} instead.
 */
public class ThreadPoolMessageHandlerInvoker extends ExecutorBasedMessageHandlerInvoker {

    private final ExecutorService executor;

    public ThreadPoolMessageHandlerInvoker(int numberOfThreads, int queueCapacity, ConsumerExecutorFactory consumerExecutorFactory) {
        super(consumerExecutorFactory);
        this.executor = consumerExecutorFactory.createConsumerThreadPool(numberOfThreads, queueCapacity);
    }

    @Override
    protected void doSubmitTask(MessageProcessingTask task, Message message) {
        executor.submit(task);
    }

    @Override
    public void shutdown() {
        doShutdown(executor);
    }

    protected void doShutdown(ExecutorService executor) {
        Utils.gracefulShutdown(executor, "consumer");
    }
}