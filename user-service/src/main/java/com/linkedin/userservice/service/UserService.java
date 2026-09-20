package com.linkedin.userservice.service;

import com.linkedin.userservice.dto.UserResponse;
import com.linkedin.userservice.entity.Connection;
import com.linkedin.userservice.entity.ConnectionStatus;
import com.linkedin.userservice.entity.User;
import com.linkedin.userservice.repository.ConnectionRepository;
import com.linkedin.userservice.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
  private final ConnectionRepository connectionRepository;
  private final UserRepository userRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final S3Service s3Service;

  private static final String CONNECTION_REQUESTED_TOPIC = "connection.requested";
  private static final String CONNECTION_ACCEPTED_TOPIC = "connection.accepted";
  private static final String USER_UPDATED_TOPIC = "user.updated";

  public String sendConnectionRequest(String receiverId, String requesterId) {
    if (connectionRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId)) {
      throw new RuntimeException("Connection request already sent");
    }

    Connection connection = new Connection();
    connection.setRequesterId(requesterId);
    connection.setReceiverId(receiverId);
    connection.setStatus(ConnectionStatus.PENDING);

    connectionRepository.save(connection);

    Map<String, Object> connectionRequestEvent = new HashMap<>();
    connectionRequestEvent.put("requesterId", requesterId);
    connectionRequestEvent.put("receiverId", receiverId);
    kafkaTemplate.send(CONNECTION_REQUESTED_TOPIC, requesterId, connectionRequestEvent);

    log.info("Connection request sent {} -> {}", requesterId, receiverId);
    return "Connection Request Sent";
  }

  public String acceptConnectionRequest(String connectionId, String requestingUserId) {
    Connection connection =
        connectionRepository
            .findById(connectionId)
            .orElseThrow(() -> new RuntimeException("Connetion not found:" + connectionId));

    connection.setStatus(ConnectionStatus.CONNECTED);
    connectionRepository.save(connection);

    Map<String, Object> connectionAcceptedEvent = new HashMap<>();
    connectionAcceptedEvent.put("requesterId", connection.getRequesterId());
    connectionAcceptedEvent.put("receiverId", connection.getReceiverId());

    kafkaTemplate.send(
        CONNECTION_ACCEPTED_TOPIC, connection.getRequesterId(), connectionAcceptedEvent);

    log.info("Connection Accepted : {}", connectionId);
    return "Connection Accepted";
  }

  public List<UserResponse> getConnections(String userId) {
    List<Connection> connections =
        connectionRepository.findByRequesterIdAndStatus(userId, ConnectionStatus.CONNECTED);

    return connections.stream()
        .map(c -> getUserProfile(c.getReceiverId()))
        .collect(Collectors.toList());
  }

  public UserResponse getUserProfile(String userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found :" + userId));

    return mapToResponse(user);
  }

  public UserResponse updateProfile(String userId, UserResponse request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found:" + userId));
    user.setHeadline(request.getHeadline());
    user.setAbout(request.getAbout());
    user.setLocation(request.getLocation());
    user.setSkills(request.getSkills());

    User savedUser = userRepository.save(user);

    Map<String, Object> userUpdatedEvent = new HashMap<>();
    userUpdatedEvent.put("userId", savedUser.getId());
    userUpdatedEvent.put("firstName", savedUser.getFirstName());
    userUpdatedEvent.put("lastName", savedUser.getLastName());
    userUpdatedEvent.put("headline", savedUser.getHeadline());
    userUpdatedEvent.put("location", savedUser.getLocation());
    userUpdatedEvent.put("skills", savedUser.getSkills());

    kafkaTemplate.send(USER_UPDATED_TOPIC, savedUser.getId(), userUpdatedEvent);

    log.info("user.updated event published: {}", savedUser.getId());

    return mapToResponse(savedUser);
  }

  public UserResponse uploadProfilePhoto(String userId, MultipartFile file) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found:" + userId));

    String photoUrl = s3Service.uploadFile(file, "profiles/" + userId + "/avatar");

    user.setProfilePhotoUrl(photoUrl);
    User savedUser = userRepository.save(user);

    log.info("Profile photo uploaded for user: {}", userId);
    return mapToResponse(savedUser);
  }

  private UserResponse mapToResponse(User user) {
    UserResponse response = new UserResponse();
    response.setId(user.getId());
    response.setEmail(user.getEmail());
    response.setFirstName(user.getFirstName());
    response.setLastName(user.getLastName());
    response.setHeadline(user.getHeadline());
    response.setAbout(user.getAbout());
    response.setLocation(user.getLocation());
    response.setProfilePhotoUrl(user.getProfilePhotoUrl());
    response.setCoverPhotoUrl(user.getCoverPhotoUrl());
    response.setRole(user.getRole());
    response.setSkills(user.getSkills());
    response.setCreatedAt(user.getCreatedAt());

    return response;
  }

  public List<Connection> getPendingConnections(String userId) {
    return connectionRepository.findByReceiverIdAndStatus(userId, ConnectionStatus.PENDING);
  }
}
