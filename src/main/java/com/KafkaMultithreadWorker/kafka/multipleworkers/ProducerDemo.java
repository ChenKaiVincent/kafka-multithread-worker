package com.KafkaMultithreadWorker.kafka.multipleworkers;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class ProducerDemo {

    public static void main(String[] args) {

        // please set the port of kafka-docker_kafka_1
        String bootstrapServers = "localhost:9093";
        String topicName = "dst_topic";
        System.out.println("boostrapServer: " + bootstrapServers);

        Properties prop = createProducerConfig(bootstrapServers);

        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(prop);

        ProducerRecord<String, String> record =
                new ProducerRecord<String, String>(topicName, "hello world");

        producer.send(record, new Callback() {
            public void onCompletion(RecordMetadata metadata, Exception e) {
                if (e != null) {
                    e.printStackTrace();
                }
                System.out.println("Sent:" + record.value() + ", Partition: " + metadata.partition() + ", Offset: "
                        + metadata.offset());
            }
        });
        producer.close();
    }

    private static Properties createProducerConfig(String bootstrapServers) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return props;
    }
}
