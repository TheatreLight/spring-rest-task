package com.hotel.bookingservice.service;

import com.hotel.bookingservice.dto.*;
import com.hotel.bookingservice.entity.*;
import com.hotel.bookingservice.exception.*;
import com.hotel.bookingservice.mapper.UserMapper;
import com.hotel.bookingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<UserDto> getAllUsers() {
        log.debug("Fetching all users");
        return userMapper.toDtoList(userRepository.findAll());
    }

    public Page<UserDto> getUsers(Pageable pageable) {
        log.debug("Fetching users with pagination: {}", pageable);
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }

    public UserDto getUserById(Long id) {
        log.debug("Fetching user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toDto(user);
    }

    public UserDto getUserByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        log.info("Creating new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        user = userRepository.save(user);
        log.info("User created successfully with id: {}", user.getId());
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto updateUser(UpdateUserRequest request) {
        log.info("Updating user with id: {}", request.getId());

        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getId()));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new DuplicateResourceException("Username '" + request.getUsername() + "' already exists");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        user = userRepository.save(user);
        log.info("User updated successfully: {}", user.getId());
        return userMapper.toDto(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully: {}", id);
    }
}
