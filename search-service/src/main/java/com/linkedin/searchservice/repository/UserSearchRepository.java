package com.linkedin.searchservice.repository;

import com.linkedin.searchservice.model.UserDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface UserSearchRepository extends ElasticsearchRepository<UserDocument, String> {
  @Query(
      "{\"multi_match\":{\"query\":\"?0\","
          + "\"fields\":[\"firstName\",\"lastName\","
          + "\"headline\",\"location\"]}}")
  List<UserDocument> searchUsers(String query);

  List<UserDocument> findBySkillsContaining(String skill);
}
