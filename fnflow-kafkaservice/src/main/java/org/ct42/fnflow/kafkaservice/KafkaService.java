/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ct42.fnflow.kafkaservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * @author Claas Thiele
 */
@Service
@RequiredArgsConstructor
public class KafkaService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DefaultKafkaConsumerFactory<String, String> defaultKafkaConsumerFactory;
    private final KafkaAdmin kafkaAdmin;
    private final ObjectMapper objectMapper;

    public void write(String topic, Integer partition, Message[] messages) {
        for (Message m : messages) {
            if(m.getHeaders() != null) {
                List<Header> headers = new ArrayList<>();
                Arrays.stream(m.getHeaders()).forEach(h ->
                        headers.add(new RecordHeader(h.getKey(), h.getValue().getBytes(StandardCharsets.UTF_8))));
                kafkaTemplate.send(new ProducerRecord<>(topic, partition, m.getKey(), m.getValue().toString(), headers));
            } else {
                kafkaTemplate.send(new ProducerRecord<>(topic, partition, m.getKey(), m.getValue().toString()));
            }
        }
    }

    public TopicInfoDTO getTopicInfo(String name) {
        kafkaTemplate.setConsumerFactory(defaultKafkaConsumerFactory);
        TopicInfoDTO topicInfo = new TopicInfoDTO();

        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> earliestResults;
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestResults;
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            List<TopicPartition> topicPartitions = adminClient.describeTopics(List.of(name)).topicNameValues().get(name).get()
                    .partitions().stream().map(i -> new TopicPartition(name, i.partition()))
                    .toList();
            Map<TopicPartition, OffsetSpec> earliest = new HashMap<>();
            Map<TopicPartition, OffsetSpec> latest = new HashMap<>();
            topicPartitions.forEach(p -> {
                earliest.put(p, OffsetSpec.earliest());
                latest.put(p, OffsetSpec.latest());
            });

            earliestResults = adminClient.listOffsets(earliest).all().get();
            latestResults = adminClient.listOffsets(latest).all().get();

            List<Integer> nodeIds = adminClient.describeCluster().nodes().get().stream().map(Node::id).toList();
            Map<Integer, Map<String, LogDirDescription>> logDirDescs = adminClient.describeLogDirs(nodeIds).allDescriptions().get();

            earliestResults.forEach((p, ei) -> {
                ListOffsetsResult.ListOffsetsResultInfo li = latestResults.get(p);

                ConsumerRecord<String, String> latestRecord = kafkaTemplate.receive(p.topic(), p.partition(), Math.max(0, li.offset() -1), Duration.ofMillis(500));
                if(latestRecord != null) {
                    topicInfo.setLastUpdated(Math.max(topicInfo.getLastUpdated(), latestRecord.timestamp()));
                }

                topicInfo.setMessageCount(topicInfo.getMessageCount() + (li.offset() - ei.offset()));

                logDirDescs.forEach((k, v) -> v.forEach((k2, v2) -> {
                    ReplicaInfo replicaInfo = v2.replicaInfos().get(p);
                    if (replicaInfo != null) {
                        topicInfo.setSizeOnDiskBytes(topicInfo.getSizeOnDiskBytes() + replicaInfo.size());
                    }
                }));
            });

        } catch (ExecutionException | InterruptedException e) {
            throw new IllegalStateException("Getting topic info failed", e);
        }

        return topicInfo;
    }

    public void deleteTopic(String topic) {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            adminClient.deleteTopics(List.of(topic)).all().get();
        } catch (ExecutionException | InterruptedException e) {
            throw new IllegalStateException("Deleting topic failed", e);
        }
    }

    public ReadBatchDTO read(String topic, int partition, String from, String to) {
        long fromOffset = 0;
        long toOffset = Long.MAX_VALUE;
        TopicPartition topicPartition = new TopicPartition(topic, partition);

        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            if (from == null) { // no value given
                fromOffset = 0;
            } else if (from.startsWith("ts")) { // timestamp
                fromOffset = getTsOffset(from, topicPartition, adminClient);
            } else if(from.contains("-")) { // iso date
                fromOffset = getDateOffset(from, topicPartition, adminClient);
            }  else { // offset
                fromOffset = Long.parseLong(from);
            }

        if (to == null) { // no value given
                toOffset = Long.MAX_VALUE;
            } else if (to.startsWith("ts")) { // timestamp
                toOffset = getTsOffset(to, topicPartition, adminClient) - 1;
            } else if(to.contains("-")) { // iso date
                toOffset = getDateOffset(to, topicPartition, adminClient);
            } else { // offset
                toOffset = Long.parseLong(to);
            }
        }

        final long toOffsetL = toOffset;

        ReadBatchDTO batch = new ReadBatchDTO();
        List<ConsumerRecord<String, String>> records = receive(topicPartition, fromOffset, Duration.ofMillis(500));
        batch.setMessages(records
            .stream()
            .filter(r -> r.offset() <= toOffsetL)
            .map(r -> {
            ReadMessage message = new ReadMessage();
            message.setKey(r.key());
            message.setOffset(r.offset());
            message.setTimestamp(r.timestamp());

            List<org.ct42.fnflow.kafkaservice.Header> headers = new ArrayList<>();
            r.headers().forEach(h -> {
                org.ct42.fnflow.kafkaservice.Header header = new org.ct42.fnflow.kafkaservice.Header();
                header.setKey(h.key());
                header.setValue(new String(h.value()));
                headers.add(header);
            });
            message.setHeaders(headers.toArray(new org.ct42.fnflow.kafkaservice.Header[0]));

            try {
                message.setValue(objectMapper.readValue(r.value(), JsonNode.class));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to parse value as Json", e);
            }
            return message;
        }).toArray(ReadMessage[]::new));
        return batch;
    }

    private long getDateOffset(String date, TopicPartition topicPartition, AdminClient adminClient) {
        long offset;
        long timestamp = ZonedDateTime.parse(date, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant().toEpochMilli();
        try {
            offset = adminClient.listOffsets(Map.of(topicPartition, OffsetSpec.forTimestamp(timestamp)))
                    .all().get().get(topicPartition).offset();
            if(offset == -1) {
                offset = Long.MAX_VALUE;
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Failed  determine offset for timestamp", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        return offset;
    }

    private long getTsOffset(String ts, TopicPartition topicPartition, AdminClient adminClient) {
        long offset;
        long timestamp = Long.parseLong(ts.substring(2));
        try {
            offset = adminClient.listOffsets(Map.of(topicPartition, OffsetSpec.forTimestamp(timestamp)))
                    .all().get().get(topicPartition).offset();
        } catch (InterruptedException e) {
            throw new IllegalStateException("Failed to determine offset for timestamp", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        return offset;
    }

    private List<ConsumerRecord<String, String>> receive(TopicPartition topicPartition, long from, Duration pollTimeout) {
        Properties props = new Properties();
        props.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500");
        try (Consumer<String, String> consumer = defaultKafkaConsumerFactory.createConsumer(null, null, null, props)) {
            consumer.assign(Collections.singletonList(topicPartition));
            consumer.seek(topicPartition, from);
            ConsumerRecords<String, String> records = consumer.poll(pollTimeout);
            return records.records(topicPartition);
        }
    }
}
