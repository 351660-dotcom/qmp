package com.qmp.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.member.dto.admin.CreateLevelRequest;
import com.qmp.member.entity.MemberLevel;
import com.qmp.member.mapper.MemberLevelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会员后台管理：维护会员等级。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final MemberLevelMapper memberLevelMapper;

    public Long createLevel(CreateLevelRequest request) {
        MemberLevel level = new MemberLevel();
        level.setTenantId(TenantContext.get());
        level.setLevelName(request.getLevelName());
        level.setMinGrowthValue(request.getMinGrowthValue() != null ? request.getMinGrowthValue() : 0);
        level.setBenefits(request.getBenefits() != null ? request.getBenefits().toString() : null);
        memberLevelMapper.insert(level);
        log.info("后台创建会员等级: levelId={}, name={}", level.getLevelId(), request.getLevelName());
        return level.getLevelId();
    }

    public List<MemberLevel> listLevels() {
        return memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                .orderByAsc(MemberLevel::getMinGrowthValue));
    }
}
