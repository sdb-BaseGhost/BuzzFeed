package org.sdb.buzzfeed.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    /**
     * Fan-out 消费者容器工厂
     * concurrency=3：3个消费者线程并行消费
     * 有限重试3次 + DLQ（死信队列）
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
            fanoutListenerContainerFactory(
                ConsumerFactory<String, String> consumerFactory,
                KafkaTemplate<String, String> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);

        // 最多重试3次，每次间隔固定1秒
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        // 重试3次仍然失败 -> 发送到 DLQ
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(kafkaTemplate);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // 业务异常不重试，直接进DLQ
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
