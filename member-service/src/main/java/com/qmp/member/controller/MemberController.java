package com.qmp.member.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.member.dto.MemberStatusResponse;
import com.qmp.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员中心对外接口（见 09 文档四）。
 */
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberQueryService memberQueryService;

    @GetMapping("/{userId}/status")
    public ApiResponse<MemberStatusResponse> getStatus(@PathVariable("userId") Long userId) {
        return ApiResponse.ok(memberQueryService.getStatus(userId));
    }
}
