package com.KafkaMultithreadWorker.kafka.multipleworkers;

import org.apache.commons.cli.CommandLine;

import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        // Assign variables from arguments
        CommandLine cmd = Utility.ArgumentParser(args);

        // Read kafka cluster configs
        Properties srcProps = Utility.readConfig(cmd.getOptionValue("src-kafka-conf"));
        Properties dstProps = Utility.readConfig(cmd.getOptionValue("dst-kafka-conf"));

        String startDt = cmd.getOptionValue("start-dt");
        int workerCount = Integer.parseInt(cmd.getOptionValue("worker-count"));
        int srcPartitionCount = Utility.getTopicPartitionCount(srcProps,
                                                               cmd.getOptionValue("src-topic"));
        int dstPartitionCount = Utility.getTopicPartitionCount(dstProps,
                                                               cmd.getOptionValue("dst-topic"));
        long[] startOffsets = Utility.getOffsetsFromTimestamp(srcProps,
                                                              cmd.getOptionValue("src-topic"),
                                                              srcPartitionCount,
                                                              startDt,
                                                     "GMT+9");
        String endDt = Utility.shiftDatetimeByHours(cmd.getOptionValue("start-dt"),
                                                    Integer.parseInt(cmd.getOptionValue("recovery-hours")),
                                           "GMT+9");
        long[] endOffsets = Utility.getOffsetsFromTimestamp(srcProps,
                                                            cmd.getOptionValue("src-topic"),
                                                            srcPartitionCount,
                                                            endDt,
                                                   "GMT+9");

        System.out.println("===============================");
        System.out.println("srcTopic: " + cmd.getOptionValue("src-topic") + "\n" +
                           "dstTopic: " + cmd.getOptionValue("dst-topic") + "\n" +
                           "workerCount: " + workerCount+ "\n" +
                           "srcPartitionCount: " + srcPartitionCount + "\n" +
                           "dstPartitionCount: " + dstPartitionCount);
        for(int p=0; p<startOffsets.length; p++)
            System.out.println("partition: " + p + " start offset: " + startOffsets[p]);
        for(int p=0; p<startOffsets.length; p++)
            System.out.println("partition: " + p + " end offset: " + endOffsets[p]);
        System.out.println("===============================");

        // Setup Workers
        Producer producer = new Producer(dstProps, cmd.getOptionValue("dst-topic"));
        WorkerHandler consumers = new WorkerHandler(srcProps,
                                                    producer,
                                                    cmd.getOptionValue("src-topic"),
                                                    startOffsets,
                                                    endOffsets);
        System.out.println("Start consuming data");
        consumers.execute(workerCount);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ignored) {}

        consumers.shutdown();
        producer.getInstance().flush();
        producer.getInstance().close();
        System.out.println("All Finished");
    }
}
