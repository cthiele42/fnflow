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

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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

                ConsumerRecord<String, String> latestRecord = kafkaTemplate.receive(p.topic(), p.partition(), Math.min(0, li.offset() -1), Duration.ofMillis(500));
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
            throw new IllegalStateException(e);
        }

        return topicInfo;
    }
}
