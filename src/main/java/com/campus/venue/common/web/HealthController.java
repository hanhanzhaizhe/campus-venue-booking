package com.campus.venue.common.web;

import com.campus.venue.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "0. 健康检查")
public class HealthController {

    private final DataSource dataSource;
    private final ObjectProvider<StringRedisTemplate> redis;

    public HealthController(DataSource dataSource, ObjectProvider<StringRedisTemplate> redis) {
        this.dataSource = dataSource;
        this.redis = redis;
    }

    @SecurityRequirements
    @Operation(summary = "探活，无需登录")
    @GetMapping
    public Result<Map<String, Object>> health() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", "venue-booking");
        data.put("status", "UP");
        try (Connection connection = dataSource.getConnection()) {
            data.put("database", connection.isValid(2) ? "UP" : "DOWN");
        }
        data.put("redis", pingRedis());
        return Result.ok(data);
    }

    private String pingRedis() {
        StringRedisTemplate template = redis.getIfAvailable();
        if (template == null) {
            return "DOWN";
        }
        try {
            template.hasKey("venue:health-ping");
            return "UP";
        } catch (Exception ex) {
            return "DOWN";
        }
    }
}
