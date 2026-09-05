package com.linkedin.userservice.repository;

import com.linkedin.userservice.entity.Connection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionRepository extends JpaRepository<Connection, String> {

}
