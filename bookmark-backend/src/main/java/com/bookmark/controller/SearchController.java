package com.bookmark.controller;

import com.bookmark.entity.BookmarkDocument;
import com.bookmark.entity.User;
import com.bookmark.service.SearchService;
import com.bookmark.service.UserService;
import com.bookmark.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索控制器
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "搜索接口")
public class SearchController {

    private final SearchService searchService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "搜索书签（默认混合语义搜索）")
    public Result<List<BookmarkDocument>> search(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "hybrid") String mode) {
        User currentUser = userService.getCurrentUser();

        List<BookmarkDocument> results;
        results = searchService.search(currentUser.getId(), keyword);
        return Result.success(results);
    }
}
