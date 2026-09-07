package com.campus.venue.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.venue.user.entity.User;
import com.campus.venue.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeedPasswordInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedPasswordInitializer.class);
    private static final String PLACEHOLDER = "PENDING_BCRYPT";
    private static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public SeedPasswordInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<User> pending = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getPasswordHash, PLACEHOLDER));
        if (pending.isEmpty()) {
            return;
        }
        String hash = passwordEncoder.encode(DEFAULT_PASSWORD);
        for (User user : pending) {
            user.setPasswordHash(hash);
            userMapper.updateById(user);
        }
        log.info("已将 {} 个占位密码更新为 BCrypt", pending.size());
    }
}
