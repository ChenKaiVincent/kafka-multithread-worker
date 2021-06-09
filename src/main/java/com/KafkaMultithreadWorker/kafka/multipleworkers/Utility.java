package com.KafkaMultithreadWorker.kafka.multipleworkers;

import org.apache.commons.cli.*;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {
    /**
     * Returns a Properties object which read from a config file.
     */
    public static Properties readConfig(String fileName) {
        Properties props = new Properties();
        InputStream inStream = null;
        try {
            inStream = new FileInputStream(fileName);
        } catch (FileNotFoundException ex) {
        }
        try {
            props.load(inStream);
        } catch (IOException ex) {
        }
        return props;
    }

    /**
     * Returns a CommandLine object which includes all arguments.
     * This function allows you to parse arguments like CLI. e.g. --key value
     */
    public static CommandLine ArgumentParser(String[] args) {
        String[] argsKeys = {"src-topic", "dst-topic", "worker-count", "start-dt",
                             "recovery-hours", "src-kafka-conf", "dst-kafka-conf"};

        Options options = new Options();
        for (String key : argsKeys) {
            String shortKey = key.replace("-", "");
            Option opt = new Option(shortKey, key, true, key);
            opt.setRequired(true);
            options.addOption(opt);
        }

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        for (String key: argsKeys) {
            System.out.println(cmd.getOptionValue(key));
        }
        return cmd;
    }

    /**
     * Returns a datetime with yyyy-MM-dd format after shifting a specific number of hours.
     * Timezone is in GMT format. e.g. 'GMT+9'
     */
    public static String shiftDatetimeByHours(String dt, int hours, String timezone) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH");
        dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
        long ts = 0;
        String shiftDt = "";
        try {
            ts = dateFormat.parse(dt).getTime();
            long hourShift = hours * 60 * 60 * 1000L;
            Date date = new Date(ts + hourShift);
            shiftDt = dateFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return shiftDt;
    }

    /**
     * Returns partition count for a specific topic.
     */
    public static int getTopicPartitionCount(Properties props, String topic) {
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
        System.out.println("partition count: " + partitions);
        return partitions;
    }

    /**
     * Returns a Long array containing all the offsets at a specified time.
     * The offset values are in the order of partition number. (from partition-0 to partition-n)
     */
    public static long[] getOffsetsFromTimestamp(Properties props, String topic,
                                                 int partitionCount, String dt, String timezone) {
        // Convert date to timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH");
        dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
        long ts = 0;
        try {
            if (!Pattern.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}$", dt)) {
                throw new Exception("Invalid dt format");
            }
            ts = dateFormat.parse(dt).getTime();
            System.out.println("ts = "+ ts);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Find all the offsets at a specified time.
        long[] offsetsArray = new long[partitionCount];
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);) {
            Map<TopicPartition, Long> timestamps = new HashMap<>();
            for (int p = 0; p < partitionCount; p++) {
                timestamps.put(new TopicPartition(topic, p), ts);
            }
            Map<TopicPartition, OffsetAndTimestamp> offsets = consumer.offsetsForTimes(timestamps);
            for (Map.Entry<TopicPartition, OffsetAndTimestamp> entry: offsets.entrySet()) {
                System.out.println("Key = " + entry.getKey() +
                        ", Value = " + entry.getValue());
                // e.g. Key = src_topic-0, Value = (timestamp=1620179926407, leaderEpoch=null, offset=2)
                // parse partition number from entry key
                Pattern pattern = Pattern.compile(topic + "-(.*?)$");
                Matcher matcher = pattern.matcher(entry.getKey().toString());
                if (!matcher.find())
                    throw new Exception("Parse partition number failed.");
                // assign offset to the array in partition order i.e. [offset-1, offset-2, ...]
                offsetsArray[Integer.parseInt(matcher.group(1).trim())] = entry.getValue().offset();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return offsetsArray;
    }

    /**
     * Returns a random number in the range from 0 to the latest partition number.
     */
    public static int randomPartition(int partitionCount) {
        return ThreadLocalRandom.current().nextInt(0, partitionCount);
    }

    /**
     * Returns true if the input array contains the specific value.
     */
    public static <T> boolean contains(final T[] array, final T v) {
        for (final T e : array)
            if (Objects.equals(v, e))
                return true;
        return false;
    }
}
