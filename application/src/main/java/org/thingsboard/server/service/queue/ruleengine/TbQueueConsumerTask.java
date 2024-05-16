/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.queue.ruleengine;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class TbQueueConsumerTask {

    @Getter
    private final Object key;
    private volatile TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> consumer;
    private volatile Supplier<TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>>> consumerSupplier;

    @Setter
    private Future<?> task;

    public TbQueueConsumerTask(Object key, Supplier<TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>>> consumerSupplier) {
        this.key = key;
        this.consumer = null;
        this.consumerSupplier = consumerSupplier;
    }

    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> getConsumer() {
        if (consumer == null) {
            synchronized (this) {
                if (consumer == null) {
                    Objects.requireNonNull(consumerSupplier, "consumerSupplier for key [" + key + "] is null");
                    consumer = consumerSupplier.get();
                    Objects.requireNonNull(consumer, "consumer for key [" + key + "] is null");
                    consumerSupplier = null;
                }
            }
        }
        return consumer;
    }

    public void subscribe(Set<TopicPartitionInfo> partitions) {
        log.trace("[{}] Subscribing to partitions: {}", key, partitions);
        getConsumer().subscribe(partitions);
    }

    public void initiateStop() {
        log.debug("[{}] Initiating stop", key);
        getConsumer().stop();
    }

    public void awaitCompletion() {
        log.trace("[{}] Awaiting finish", key);
        if (isRunning()) {
            try {
                task.get(30, TimeUnit.SECONDS);
                log.trace("[{}] Awaited finish", key);
            } catch (Exception e) {
                log.warn("[{}] Failed to await for consumer to stop", key, e);
            }
            task = null;
        }
    }

    public boolean isRunning() {
        return task != null;
    }

}
