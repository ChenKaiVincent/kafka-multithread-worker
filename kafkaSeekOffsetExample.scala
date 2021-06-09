import java.time.Duration
import java.util.Properties
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import collection.JavaConverters._

object OffsetsForTime extends App {

  implicit def toJavaOffsetQuery(offsetQuery: Map[TopicPartition, scala.Long]): java.util.Map[TopicPartition, java.lang.Long] =
    offsetQuery
      .map { case (tp, time) => tp -> new java.lang.Long(time) }
      .asJava

  val topic = "myInTopic"
  val timestamp: Long = 1595971151000L

  val props = new Properties()
  props.put("group.id", "group-id1337")
  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("auto.offset.reset", "earliest")
  val consumer: KafkaConsumer[String, String] = new KafkaConsumer[String, String](props)

  val topicPartition = new TopicPartition(topic, 0)
  consumer.assign(java.util.Collections.singletonList(topicPartition))
  // dummy poll before calling seek
  consumer.poll(Duration.ofMillis(500))

  // get next available offset after given timestamp
  val (_, offsetAndTimestamp) = consumer.offsetsForTimes(Map(topicPartition -> timestamp)).asScala.head
  // seek to offset
  consumer.seek(topicPartition, offsetAndTimestamp.offset())

  // poll data
  val record = consumer.poll(Duration.ofMillis(500)).asScala.toList

  for (data <- record) {
    println(s"Timestamp: ${data.timestamp()}, Key: ${data.key()}, Value: ${data.value()}")
  }

}