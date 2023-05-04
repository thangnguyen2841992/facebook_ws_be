package com.example.demo.controller;
import com.example.demo.model.dto.*;
import com.example.demo.model.entity.*;
import com.example.demo.service.chat.IChatService;
import com.example.demo.service.comment.ICommentService;
import com.example.demo.service.image.IImageService;
import com.example.demo.service.likeComment.ILikeCommentService;
import com.example.demo.service.likePost.ILikePostService;
import com.example.demo.service.notification.INotificationService;
import com.example.demo.service.post.IPostService;
import com.example.demo.service.reply.IReplyService;
import com.example.demo.service.status.IStatusService;
import com.example.demo.service.user.IUSerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin("*")
public class WebsocketRestController {
    @Autowired
    private IChatService chatService;
    @Autowired
    private IUSerService uSerService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private IImageService imageService;
    @Autowired
    private IPostService postService;
    @Autowired
    private ILikePostService likePostService;
    @Autowired
    private INotificationService notificationService;
    @Autowired
    private ICommentService commentService;
    @Autowired
    private IReplyService replyService;
    @Autowired
    private ILikeCommentService likeCommentService;


    @MessageMapping("/posts")
    @SendTo("/topic/posts")
    public PostResponseDto handleMessage(PostForm postForm) {
        // Xử lý request message và trả về response message
        Post post = new Post();
        Optional<User> userOptional = this.uSerService.findById(Long.valueOf(postForm.getUserId()));
        userOptional.ifPresent(post::setUser);
        Optional<StatusPost> statusPostOptional = this.statusService.findById(Long.valueOf(postForm.getStatusId()));
        statusPostOptional.ifPresent(post::setStatusPost);
        post.setContent(postForm.getContent());
        post.setDateCreated(new Date());
        this.postService.save(post);
        for (int i = 0; i < postForm.getImages().length; i++) {
            this.imageService.save(new Images(postForm.getImages()[i],post));
        }
        PostResponseDto postResponseDto = this.postService.findPostById(post.getId());
        postResponseDto.setTotalLikePost(0);
        postResponseDto.setTotalComments(0);
        List<CommentDTO> commentDTOList = new ArrayList();
        postResponseDto.setComments(commentDTOList);
       return postResponseDto;
    }

    @MessageMapping("/likes")
    @SendTo("/topic/likes")
    public CheckAddLike handleMessage(LikePostRequest likePostRequest) {
        CheckAddLike checkAddLike = new CheckAddLike();
        checkAddLike.setPostId(Long.valueOf(likePostRequest.getPostId()));
        LikePost newLikePost = new LikePost();
        Optional<LikePost> likePostOptional = this.likePostService.findLikeByUserIdAndPostId(Long.valueOf(likePostRequest.getCurrentUserId()), Long.valueOf(likePostRequest.getPostId()));
        if (!likePostOptional.isPresent()) {
            Optional<User> userOptional = this.uSerService.findById(Long.valueOf(likePostRequest.getCurrentUserId()));
            userOptional.ifPresent(newLikePost::setFromUser);
            Optional<Post> optionalPost = this.postService.findById(Long.valueOf(likePostRequest.getPostId()));
            optionalPost.ifPresent(newLikePost::setPost);
            checkAddLike.setUserId(optionalPost.get().getUser().getId());
            this.likePostService.save(newLikePost);
            checkAddLike.setAdd(true);
            if (userOptional.get().getId() != optionalPost.get().getUser().getId()) {
                Notification newNotification = new Notification();
                newNotification.setDateCreated(new Date());
                newNotification.setContent(newLikePost.getFromUser().getFullName()+ " đã thích status của bạn!");
                newNotification.setToUser(optionalPost.get().getUser());
                newNotification.setFromUser(userOptional.get());
                newNotification.setStatus(0);
                this.notificationService.save(newNotification);
            }
        } else {
            this.likePostService.remove(likePostOptional.get().getId());
            checkAddLike.setAdd(false);
        }
        return checkAddLike;
    }
    @MessageMapping("/comments")
    @SendTo("/topic/comments")
    public CommentDTO handleMessage(CommentRequest commentRequest) {
        Optional<User> userOptional = this.uSerService.findById(Long.valueOf(commentRequest.getCurrentUserID()));
        Optional<Post> postOptional = this.postService.findById(Long.valueOf(commentRequest.getPostID()));
        Comment newComment = new Comment();
        newComment.setDateCreated(new Date());
        postOptional.ifPresent(newComment::setPost);
        userOptional.ifPresent(newComment::setFormUser);
        newComment.setContent(commentRequest.getContent());
        this.commentService.save(newComment);
        if (userOptional.get().getId() != postOptional.get().getUser().getId()) {
            Notification newNotification = new Notification();
            newNotification.setDateCreated(new Date());
            newNotification.setContent(newComment.getFormUser().getFullName()+ " đã bình luận status của bạn!");
            newNotification.setToUser(postOptional.get().getUser());
            newNotification.setFromUser(userOptional.get());
            newNotification.setStatus(2);
            this.notificationService.save(newNotification);
        }
        CommentDTO commentDTO = this.commentService.mapperCommentEntityToDto(newComment);
        return commentDTO;
    }

    @MessageMapping("/replies")
    @SendTo("/topic/replies")
    public ReplyDTO handleMessage(ReplyRequest replyRequest) {
        Reply newReply = new Reply();
        Optional<Post> postOptional = this.postService.findById(Long.valueOf(replyRequest.getPostID()));
        Optional<User> userOptional = this.uSerService.findById(Long.valueOf(replyRequest.getCurrentUserID()));
        Optional<Comment> commentOptional = this.commentService.findById(Long.valueOf(replyRequest.getCommentID()));
        newReply.setDateCreated(new Date());
        newReply.setContent(replyRequest.getContent());
        postOptional.ifPresent(newReply::setPost);
        userOptional.ifPresent(newReply::setFromUser);
        commentOptional.ifPresent(newReply::setComment);
        this.replyService.save(newReply);
        if (userOptional.get().getId() != commentOptional.get().getFormUser().getId()) {
            Notification newNotification = new Notification();
            newNotification.setDateCreated(new Date());
            newNotification.setContent(newReply.getFromUser().getFullName()+ " đã phản hồi bình luận của bạn!");
            newNotification.setToUser(commentOptional.get().getFormUser());
            newNotification.setFromUser(userOptional.get());
            newNotification.setStatus(3);
            this.notificationService.save(newNotification);
        }
        return this.replyService.mapperReplyEntityToDto(newReply);
    }

    @MessageMapping("/likeComments")
    @SendTo("/topic/likeComments")
    public CheckAddLikeComment handleMessage(LikeCommentRequest likeCommentRequest) {
        CheckAddLikeComment checkAddLikeComment = new CheckAddLikeComment();
        checkAddLikeComment.setPostId(Long.valueOf(likeCommentRequest.getPostID()));
        checkAddLikeComment.setCommentId(Long.valueOf(likeCommentRequest.getCommentID()));
        Optional<LikeComment> likeCommentOptional = this.likeCommentService.findLikeCommentByCommentIdAndUserId(Long.valueOf(likeCommentRequest.getCurrentUserID()), Long.valueOf(likeCommentRequest.getCommentID()));
        LikeComment likeComment = new LikeComment();
        if (!likeCommentOptional.isPresent()) {
            Optional<User> userOptional = this.uSerService.findById(Long.valueOf(likeCommentRequest.getCurrentUserID()));
            Optional<Comment> commentOptional = this.commentService.findById(Long.valueOf(likeCommentRequest.getCommentID()));
            Optional<Post> postOptional = this.postService.findById(Long.valueOf(likeCommentRequest.getPostID()));
            likeComment.setFromUser(userOptional.get());
            likeComment.setPost(postOptional.get());
            likeComment.setComment(commentOptional.get());
            this.likeCommentService.save(likeComment);
            checkAddLikeComment.setAdd(true);

            if (userOptional.get().getId() != commentOptional.get().getFormUser().getId()) {
                Notification newNotification = new Notification();
                newNotification.setDateCreated(new Date());
                newNotification.setContent(likeComment.getFromUser().getFullName()+ " đã thích bình luận của bạn!");
                newNotification.setToUser(commentOptional.get().getFormUser());
                newNotification.setFromUser(userOptional.get());
                newNotification.setStatus(1);
                this.notificationService.save(newNotification);
            }
        } else {
            this.likeCommentService.remove(likeCommentOptional.get().getId());
            checkAddLikeComment.setAdd(false);
        }
        return checkAddLikeComment;
    }
}