package com.KafkaMultithreadWorker.kafka.multipleworkers;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.KafkaFuture;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class Producer {

        private final KafkaProducer<String, String> producer;
        private final String topic;
        private final int partitionCount;

        public Producer(Properties props, String topic) {
            this.producer = new KafkaProducer<String, String>(props);
            this.topic = topic;
            this.partitionCount = this.getTopicPartitionCount(props, topic);
        }

        private int getTopicPartitionCount(Properties props, String topic) {
            AdminClient client = AdminClient.create(props);

            DescribeTopicsResult result = client.describeTopics(Arrays.asList(topic));
            Map<String, KafkaFuture<TopicDescription>> values = result.values();
            KafkaFuture<TopicDescription> topicDescription = values.get(topic);
            int partitions = 0;
            try {
                partitions = topicDescription.get().partitions().size();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return partitions;
        }

        public String getTopicName() {
            return this.topic;
        }

        public int getPartitionCount() {
            return this.partitionCount;
        }

        public KafkaProducer getInstance() {
            return this.producer;
        }
}

