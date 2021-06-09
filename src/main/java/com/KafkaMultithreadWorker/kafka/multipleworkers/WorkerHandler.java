package com.KafkaMultithreadWorker.kafka.multipleworkers;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.util.Arrays;
import java.util.Set;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WorkerHandler {

    private final KafkaConsumer<String, String> consumer;
    private final Producer producer;
    private ExecutorService executors;
    private long[] endOffsets;
    private Boolean[] donePartitionsList;
    private boolean allPartitionsDone = false;

    public WorkerHandler(Properties props, Producer producer, String topic,
                         long[] startOffsets, long[] endOffsets) {
        this.producer = producer;
        this.endOffsets = endOffsets;
        this.donePartitionsList = new Boolean[startOffsets.length];
        Arrays.fill(this.donePartitionsList, false);

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topic));

        // Seek to the start offset for each partition
        consumer.poll(2000);
        Set<TopicPartition> assignment = consumer.assignment();
        System.out.println("The original assignment is: " + assignment);
        int p = 0;
        System.out.println("The new assignment is: ");
        for (TopicPartition tp : assignment) {
            consumer.seek(tp, startOffsets[p]);
            System.out.println("Topic partition: " + tp.toString() + " startOffset: " + startOffsets[p]);
            p++;
        }
    }

    public void execute(int workerNum) {
        executors = new ThreadPoolExecutor(workerNum, workerNum, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1000), new ThreadPoolExecutor.CallerRunsPolicy());

        while (true) {
            // Keep polling records
            ConsumerRecords<String, String> records = consumer.poll(200);
            for (final ConsumerRecord record : records) {
                // Skip if this partition is done
                if (this.donePartitionsList[record.partition()] == true)
                    continue;
                // Skip if record offset is over the end offset
                if (record.offset() >= this.endOffsets[record.partition()]) {
                    this.donePartitionsList[record.partition()] = true;
                    continue;
                }
                executors.submit(new Worker(producer, record));
            }

            // Check if all partitions are done
            if (!Utility.contains(this.donePartitionsList, false )) {
                System.out.println("All partitions have reached their end offsets");
                break;
            }
        }
        shutdown();
    }

    public void shutdown() {
        if (consumer != null) {
            System.out.println("Consumer closed.");
            consumer.close();
        }
        if (executors != null) {
            System.out.println("Executor shutdown.");
            executors.shutdown();
        }
        try {
            if (!executors.awaitTermination(300, TimeUnit.SECONDS)) {
                System.out.println("Timeout. Ignore for this case");
            }
        } catch (InterruptedException ignored) {
            System.out.println("Other thread interrupted this shutdown, ignore for this case.");
            Thread.currentThread().interrupt();
        }
    }
}
