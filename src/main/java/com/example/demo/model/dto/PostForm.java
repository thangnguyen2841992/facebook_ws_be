package com.example.demo.model.dto;

import com.example.demo.model.entity.StatusPost;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostForm {
    private String content;

    private String statusId;

    private String userId;

    private String[] images;
}
