package com.example.demo.model.dto;

import com.example.demo.model.entity.Comment;
import com.example.demo.model.entity.Post;
import com.example.demo.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.ManyToOne;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LikeCommentRequest {

    private String currentUserID;

    private String postID;

    private String commentID;

}
