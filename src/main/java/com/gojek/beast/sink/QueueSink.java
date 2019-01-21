package com.gojek.beast.sink;

import com.gojek.beast.config.QueueConfig;
import com.gojek.beast.models.FailureStatus;
import com.gojek.beast.models.Records;
import com.gojek.beast.models.Status;
import com.gojek.beast.models.SuccessStatus;
import com.gojek.beast.stats.Stats;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.concurrent.BlockingQueue;

@AllArgsConstructor
public class QueueSink implements Sink {
    private final Stats statsClient = Stats.client();
    private final BlockingQueue<Records> recordQueue;
    private final QueueConfig config;

    @Override
    public Status push(Records messages) {
        Instant start = Instant.now();
        try {
            recordQueue.offer(messages, config.getTimeout(), config.getTimeoutUnit());
            statsClient.gauge("queue.elements,name=records", recordQueue.size());
        } catch (InterruptedException e) {
            return new FailureStatus(e);
        }
        statsClient.gauge("sink.queue.push.messages", messages.size());
        statsClient.timeIt("sink.queue.push.time", start);
        return new SuccessStatus();
    }

    @Override
    public void close(String reason) {
        recordQueue.clear();
    }
}
