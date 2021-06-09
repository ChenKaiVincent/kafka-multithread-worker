package com.KafkaMultithreadWorker.kafka.multipleconsumers;

public final class MultipleConsumersMain {

  public static void main(String[] args) {

    String brokers = "localhost:9093";
    String groupId = "group01";
    String topic = "src_topic";
    int numberOfConsumer = 2;


    if (args != null) {
      brokers = args[0];
      groupId = args[1];
      topic = args[2];
      numberOfConsumer = Integer.parseInt(args[3]);
    }
    System.out.println("Broker: " + brokers + "\n" +
                       "groupId: " + groupId + "\n" +
                       "topic: " + topic + "\n" +
                       "numberOfConsumer: " + numberOfConsumer);

    // Start Notification Producer Thread
    NotificationProducerThread producerThread = new NotificationProducerThread(brokers, topic);
    Thread t1 = new Thread(producerThread);
    t1.start();

    // Start group of Notification Consumers
    NotificationConsumerGroup consumerGroup =
        new NotificationConsumerGroup(brokers, groupId, topic, numberOfConsumer);

    consumerGroup.execute();

    try {
      Thread.sleep(100000);
    } catch (InterruptedException ie) {

    }
  }
}
