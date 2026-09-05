package com.linkedin.userservice.dto;

import com.linkedin.userservice.entity.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
  private String id;

  private String email;

  private String firstName;

  private String lastName;

  private String headline;
  private String about;
  private String location;
  private String profilePhotoUrl;
  private String coverPhotoUrl;

  private UserRole role;

  private List<String> skills;
  private LocalDateTime createdAt;
}
