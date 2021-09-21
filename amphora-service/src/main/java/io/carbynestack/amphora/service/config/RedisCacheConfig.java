/*
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository https://github.com/carbynestack/amphora.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.carbynestack.amphora.service.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisCacheConfig {

  @Bean
  public JedisConnectionFactory redisConnectionFactory(
      AmphoraCacheProperties amphoraCacheProperties) {
    RedisStandaloneConfiguration rsc = new RedisStandaloneConfiguration();
    rsc.setHostName(amphoraCacheProperties.getHost());
    rsc.setPort(amphoraCacheProperties.getPort());
    return new JedisConnectionFactory(rsc);
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    //    redisTemplate.setEnableTransactionSupport(true);
    redisTemplate.setConnectionFactory(cf);
    return redisTemplate;
  }

  @Bean
  public CacheManager cacheManager(
      RedisConnectionFactory cf, AmphoraCacheProperties amphoraCacheProperties) {
    return RedisCacheManager.builder(cf)
        .initialCacheNames(amphoraCacheProperties.getCacheNames())
        .build();
  }
}
