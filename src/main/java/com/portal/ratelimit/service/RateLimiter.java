package com.portal.ratelimit.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.time.Duration.ofMinutes;

@Service
public class RateLimiter {
    final ProxyManager<String> proxyManager;

    @Autowired
    public RateLimiter(final ProxyManager<String> lettuceProxyManager) {
        this.proxyManager = lettuceProxyManager;
    }

    public Bucket resolveBucket(String key) {
        final Bandwidth limit = Bandwidth.builder().capacity(3)
                .refillGreedy(3, ofMinutes(5))
                .build();

        BucketConfiguration bucketConfiguration = BucketConfiguration.builder().addLimit(limit).build();

        System.out.println("bucketConfiguration: " + bucketConfiguration);

        return proxyManager.builder()
                .build(key, () -> bucketConfiguration);
    }

//    private Supplier<BucketConfiguration> getConfigSupplierForUser(String userId) {
//
//        final Bandwidth limit = Bandwidth.builder().capacity(3)
//                .refillGreedy(3, Duration.ofSeconds(60))
//                .build();
//
//        return () -> (BucketConfiguration.builder()
//                .addLimit(limit)
//                .build());
//    }

}
