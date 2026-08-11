package com.safeher.backend.repository;

import com.safeher.backend.entity.Comment;
import com.safeher.backend.entity.Post;
import com.safeher.backend.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByPostAndStatusInOrderByCreatedAtAsc(
            Post post, List<PostStatus> statuses);

    long countByPostAndStatusIn(Post post, List<PostStatus> statuses);
}
