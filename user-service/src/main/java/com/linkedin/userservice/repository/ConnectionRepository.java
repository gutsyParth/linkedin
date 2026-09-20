package com.linkedin.userservice.repository;

import com.linkedin.userservice.entity.Connection;
import com.linkedin.userservice.entity.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConnectionRepository extends JpaRepository<Connection, String> {
  boolean existsByRequesterIdAndReceiverId(String requesterId, String receiverId);

  List<Connection> findByRequesterIdAndStatus(String requesterId, ConnectionStatus status);

  List<Connection> findByReceiverIdAndStatus(String requesterId, ConnectionStatus status);
}
