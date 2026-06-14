package com.qmp.member.service;

import com.qmp.member.dto.MemberStatusResponse;
import com.qmp.member.entity.MemberAccount;
import com.qmp.member.mapper.MemberAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 会员身份查询（09 文档四）。
 */
@Service
@RequiredArgsConstructor
public class MemberQueryService {

    private final MemberAccountMapper memberAccountMapper;

    public MemberStatusResponse getStatus(Long userId) {
        MemberAccount account = memberAccountMapper.selectById(userId);
        boolean isMember = account != null && Boolean.TRUE.equals(account.getIsMember());
        return MemberStatusResponse.builder()
                .userId(userId)
                .isMember(isMember)
                .build();
    }
}
