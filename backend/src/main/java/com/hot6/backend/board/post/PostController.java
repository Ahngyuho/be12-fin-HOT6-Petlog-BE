package com.hot6.backend.board.post;

import com.hot6.backend.board.post.model.PostDto;
import com.hot6.backend.board.post.model.PostListResponse;
import com.hot6.backend.common.BaseResponse;
import com.hot6.backend.common.BaseResponseStatus;
import com.hot6.backend.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {

    private final PostService postService;

//    @PostMapping(value = "/create", consumes = {"multipart/form-data"})
//    public ResponseEntity<BaseResponse<Void>> create(
//            @AuthenticationPrincipal User user,
//            @RequestPart("post") @Valid PostDto.PostRequest dto,
//            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {
//        postService.create(user,dto, images);
//        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS));
//    }

    @GetMapping("/list/{boardName}")
    public ResponseEntity<BaseResponse<PostListResponse>> list(
            @PathVariable String boardName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PostListResponse response = postService.list(boardName, page, size);
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));
    }

    @GetMapping("/read/{postIdx}")
    public ResponseEntity<BaseResponse<PostDto.PostResponse>> read(@PathVariable Long postIdx) {
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, postService.read(postIdx)));
    }

    @GetMapping("/search")
    public ResponseEntity<BaseResponse<PostListResponse>> search(
            @RequestParam String boardName,
            @RequestParam String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            System.out.println("[검색 요청] board=" + boardName + ", category=" + category + ", keyword=" + keyword + ", page=" + page);

            PostListResponse response = postService.search(boardName, category, keyword, page, size);
            return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, response));
        } catch (Exception e) {
            System.out.println("게시글 검색 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new BaseResponse<>(BaseResponseStatus.POST_SEARCH_FAILED));
        }
    }



    @DeleteMapping("/delete/{idx}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable Long idx) {
        postService.delete(idx);
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS));
    }

//    @PutMapping(value = "/update/{idx}", consumes = {"multipart/form-data"})
//    public ResponseEntity<BaseResponse<Void>> update(
//            @PathVariable Long idx,
//            @RequestPart("post") PostDto.PostRequest dto,
//            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {
//        postService.update(idx, dto, images);
//        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS));
//    }

    @GetMapping("/list/user/{userId}")
    public ResponseEntity<BaseResponse<List<PostDto.UserPostResponse>>> getUserPosts(@PathVariable Long userId) {
        return ResponseEntity.ok(new BaseResponse<>(BaseResponseStatus.SUCCESS, postService.findUserPosts(userId)));
    }
}
