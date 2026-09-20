package com.linkedin.postservice.repository;

import com.linkedin.postservice.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {
  List<Comment> findByPostIdOrderByCreatedAtDesc(String postId);
}
