package com.linkedin.searchservice.repository;

import com.linkedin.searchservice.model.PostDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, String> {
  @Query("{\"match\":{\"content\":{\"query\":\"?0\"," + "\"fizziness\":\"AUTO\"}}}")
  List<PostDocument> searchPosts(String query);
}
