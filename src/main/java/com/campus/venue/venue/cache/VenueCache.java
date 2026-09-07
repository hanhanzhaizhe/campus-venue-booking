package com.campus.venue.venue.cache;

import com.campus.venue.venue.dto.VenueResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Cache Aside：只缓存场地详情。占用查询和提交预约不走缓存。
 * Redis 不可用时降级为直接读库，不影响预约正确性。
 */
@Component
public class VenueCache {

    public static final String KEY_PREFIX = "venue:detail:";
    private static final Duration TTL = Duration.ofHours(1);
    private static final Logger log = LoggerFactory.getLogger(VenueCache.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public VenueCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public VenueResponse get(Long id) {
        try {
            String json = redis.opsForValue().get(KEY_PREFIX + id);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, VenueResponse.class);
        } catch (Exception ex) {
            log.warn("读取场地缓存失败，回源数据库: id={}", id);
            return null;
        }
    }

    public void put(VenueResponse venue) {
        if (venue == null || venue.getId() == null) {
            return;
        }
        try {
            redis.opsForValue().set(KEY_PREFIX + venue.getId(), objectMapper.writeValueAsString(venue), TTL);
        } catch (Exception ex) {
            log.warn("写入场地缓存失败: id={}", venue.getId());
        }
    }

    public void evict(Long id) {
        try {
            redis.delete(KEY_PREFIX + id);
        } catch (Exception ex) {
            log.warn("删除场地缓存失败: id={}", id);
        }
    }
}
