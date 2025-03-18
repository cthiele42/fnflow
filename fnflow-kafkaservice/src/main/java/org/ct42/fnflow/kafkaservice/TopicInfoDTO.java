package org.ct42.fnflow.kafkaservice;

import lombok.Data;

@Data
public class TopicInfoDTO {
    private long lastUpdated;
    private long messageCount;
    private long sizeOnDiskBytes;
}
