package com.qmp.member.controller;

import com.qmp.kernel.common.ApiResponse;
import com.qmp.member.dto.admin.CreateLevelRequest;
import com.qmp.member.entity.MemberLevel;
import com.qmp.member.service.AdminMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 会员后台管理接口（{@code /admin/v1}）。
 */
@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @PostMapping("/levels")
    public ApiResponse<Map<String, Object>> createLevel(@RequestBody CreateLevelRequest request) {
        return ApiResponse.ok(Map.of("level_id", adminMemberService.createLevel(request)));
    }

    @GetMapping("/levels")
    public ApiResponse<List<MemberLevel>> listLevels() {
        return ApiResponse.ok(adminMemberService.listLevels());
    }
}
