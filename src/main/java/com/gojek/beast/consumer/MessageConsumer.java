package com.gojek.beast.consumer;

import com.gojek.beast.converter.Converter;
import com.gojek.beast.models.FailureStatus;
import com.gojek.beast.models.ParseException;
import com.gojek.beast.models.Record;
import com.gojek.beast.models.Records;
import com.gojek.beast.models.Status;
import com.gojek.beast.models.SuccessStatus;
import com.gojek.beast.sink.Sink;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class MessageConsumer {

    private final KafkaConsumer<byte[], byte[]> kafkaConsumer;
    private final Sink sink;
    private final Converter recordConverter;
    private final long timeoutMillis;

    public Status consume() {
        ConsumerRecords<byte[], byte[]> messages;
        synchronized (kafkaConsumer) {
            messages = kafkaConsumer.poll(timeoutMillis);
        }
        log.info("Pulled {} messages", messages.count());
        if (messages.isEmpty()) {
            return new SuccessStatus();
        }
        List<Record> records;
        try {
            records = recordConverter.convert(messages);
        } catch (ParseException e) {
            Status failure = new FailureStatus(e);
            log.error("Error while converting messages: {}", failure.toString());
            return failure;
        }
        return sink.push(new Records(records));
    }
}
