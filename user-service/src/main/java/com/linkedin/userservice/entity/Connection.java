package com.linkedin.userservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="connections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Connection {
    private String id;
    private String requesterId;
    private String receiverId;
    private ConnectionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
