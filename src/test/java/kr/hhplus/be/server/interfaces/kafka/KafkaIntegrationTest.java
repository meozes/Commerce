package kr.hhplus.be.server.interfaces.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Testcontainers
public class KafkaIntegrationTest {

    private static final String TOPIC = "test-topic";
    private static final Network network = Network.newNetwork();


    @Container
    private final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.3.0"))
            .withNetwork(network);

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(props)) {
            NewTopic newTopic = new NewTopic(TOPIC, 1, (short) 1);
            adminClient.createTopics(Collections.singleton(newTopic)).all().get();
        }
    }

    @Test
    void testKafkaMessaging() throws Exception {
        String testMessage = "Hello, Kafka!";
        String testKey = "test-key";

        // Producer 설정
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "15000");
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");

        // Consumer 설정
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        // Producer로 메시지 전송
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, testKey, testMessage);
            producer.send(record).get();
            System.out.println("메시지 발송 완료");
        }

        // Consumer로 메시지 수신 및 검증
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            // 메시지 수신 대기
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

            // 검증
            assertFalse(records.isEmpty(), "메시지가 수신되지 않았습니다.");

            ConsumerRecord<String, String> receivedRecord = records.iterator().next();
            System.out.println("key = " + receivedRecord.key() + " value = " + receivedRecord.value());
            assertEquals(testMessage, receivedRecord.value(), "메시지 내용이 일치하지 않습니다.");
            assertEquals(testKey, receivedRecord.key(), "메시지 키가 일치하지 않습니다.");

            System.out.println("메시지 수신 성공");
        }
    }

}
