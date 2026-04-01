package com.enterprise.app.service;

import com.enterprise.app.cache.CacheService;
import com.enterprise.app.config.RedisConfig;
import com.enterprise.app.controller.UserController.CreateUserRequest;
import com.enterprise.app.controller.UserController.UpdateUserRequest;
import com.enterprise.app.exception.ResourceNotFoundException;
import com.enterprise.app.exception.DuplicateResourceException;
import com.enterprise.app.messaging.kafka.KafkaEventProducer;
import com.enterprise.app.messaging.rabbitmq.RabbitMQProducer;
import com.enterprise.app.model.dto.PageDto;
import com.enterprise.app.model.dto.UserDto;
import com.enterprise.app.model.entity.User;
import com.enterprise.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final KafkaEventProducer kafkaProducer;
    private final RabbitMQProducer  rabbitProducer;
    private final CacheService cacheService;

    // ── Read ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_USERS, key = "#id", unless = "#result == null")
    public UserDto findById(UUID id) {
        log.debug("Fetching user id={}", id);
        return userRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_USERS, key = "'email:' + #email")
    public UserDto findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }

    @Transactional(readOnly = true)
    public PageDto<UserDto> findAll(Pageable pageable, String keyword, String status) {
        Page<User> page = userRepository.findAllActiveUsers(pageable);
        return PageDto.from(page.map(this::toDto));
    }

    // ── Create ──────────────────────────────────────────────────────────────

    public UserDto create(CreateUserRequest req) {
        if (userRepository.existsByEmail(req.email()))
            throw new DuplicateResourceException("User", "email", req.email());
        if (userRepository.existsByUsername(req.username()))
            throw new DuplicateResourceException("User", "username", req.username());

        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .firstName(req.firstName())
                .lastName(req.lastName())
                .phoneNumber(req.phoneNumber())
                .build();

        user = userRepository.save(user);
        log.info("Created user id={} email={}", user.getId(), user.getEmail());

        // Publish events
        publishUserEvent("USER_CREATED", user);
        sendWelcomeEmail(user);

        return toDto(user);
    }

    // ── Update ──────────────────────────────────────────────────────────────

    @CachePut(value = RedisConfig.CACHE_USERS, key = "#id")
    public UserDto update(UUID id, UpdateUserRequest req) {
        User user = findUserOrThrow(id);
        if (req.firstName()   != null) user.setFirstName(req.firstName());
        if (req.lastName()    != null) user.setLastName(req.lastName());
        if (req.phoneNumber() != null) user.setPhoneNumber(req.phoneNumber());
        if (req.avatarUrl()   != null) user.setAvatarUrl(req.avatarUrl());

        user = userRepository.save(user);
        publishUserEvent("USER_UPDATED", user);
        return toDto(user);
    }

    // ── Delete ──────────────────────────────────────────────────────────────

    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_USERS, key = "#id"),
            @CacheEvict(value = RedisConfig.CACHE_USERS, key = "'email:' + #result", condition = "#result != null")
    })
    public void delete(UUID id) {
        User user = findUserOrThrow(id);
        userRepository.softDelete(id, LocalDateTime.now());
        log.info("Soft-deleted user id={}", id);
        publishUserEvent("USER_DELETED", user);
    }

    // ── Status Change ───────────────────────────────────────────────────────

    @CachePut(value = RedisConfig.CACHE_USERS, key = "#id")
    public UserDto changeStatus(UUID id, String status) {
        User user = findUserOrThrow(id);
        user.setStatus(User.UserStatus.valueOf(status.toUpperCase()));
        user = userRepository.save(user);
        publishUserEvent("USER_STATUS_CHANGED", user);
        return toDto(user);
    }

    // ── Login Tracking ──────────────────────────────────────────────────────

    public void recordLogin(UUID id) {
        userRepository.updateLastLogin(id, LocalDateTime.now());
        cacheService.evict(RedisConfig.CACHE_USERS, id.toString());
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));
    }

    private void publishUserEvent(String eventType, User user) {
        Map<String, Object> event = Map.of(
                "eventType", eventType,
                "userId",    user.getId().toString(),
                "email",     user.getEmail(),
                "timestamp", LocalDateTime.now().toString()
        );
        kafkaProducer.publishUserEvent(user.getId().toString(), event);
    }

    private void sendWelcomeEmail(User user) {
        Map<String, Object> emailPayload = Map.of(
                "to",      user.getEmail(),
                "subject", "Welcome to Enterprise!",
                "body",    "Hello " + user.getFirstName() + ", welcome aboard!"
        );
        rabbitProducer.sendEmail(emailPayload);
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus().name())
                .roles(user.getRoles().stream().map(Enum::name).toList())
                .emailVerified(user.isEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}