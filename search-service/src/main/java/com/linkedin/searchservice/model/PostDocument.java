package com.linkedin.searchservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDocument {
  @Id private String id;

  @Field(type = FieldType.Text)
  private String content;

  @Field(type = FieldType.Keyword)
  private String authorId;

  private String imageUrl;
  private String createdAt;
}
