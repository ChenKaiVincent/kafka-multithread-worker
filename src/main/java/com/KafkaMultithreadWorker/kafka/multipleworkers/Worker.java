package com.KafkaMultithreadWorker.kafka.multipleworkers;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

public class Worker implements Runnable {
    private final ConsumerRecord<String, String> consumerRecord;
    private final Producer producer;

    public Worker(Producer producer, ConsumerRecord record) {
        this.producer = producer;
        this.consumerRecord = record;
    }

    private ProducerRecord convertToProducerRecord(ConsumerRecord consumerRecord) {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<String, String>(
                this.producer.getTopicName(),
                Utility.randomPartition(this.producer.getPartitionCount()),
                this.consumerRecord.timestamp(),
                this.consumerRecord.key(),
                this.consumerRecord.value());
        return producerRecord;
    }

    @Override
    public void run() {
        System.out.println("Receive message: " + consumerRecord.value() +
                           ", message timestamp: " + consumerRecord.timestamp() +
                           ", Partition: " + consumerRecord.partition() +
                           ", Offset: " + consumerRecord.offset() +
                           ", by ThreadID: " + Thread.currentThread().getId());

        ProducerRecord producerRecord = convertToProducerRecord(consumerRecord);
        producer.getInstance().send(producerRecord);
    }
}
