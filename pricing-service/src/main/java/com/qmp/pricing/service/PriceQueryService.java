package com.qmp.pricing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.common.BizException;
import com.qmp.pricing.dto.PriceResponse;
import com.qmp.pricing.entity.PriceCalendar;
import com.qmp.pricing.error.PricingErrorCode;
import com.qmp.pricing.mapper.PriceCalendarMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 价格查询（09 文档三），供 order-service 创建订单时取价快照。
 */
@Service
@RequiredArgsConstructor
public class PriceQueryService {

    private final PriceCalendarMapper priceCalendarMapper;

    public PriceResponse getPrice(Long skuId, LocalDate saleDate, boolean isMember) {
        String priceType = isMember ? "MEMBER" : "RETAIL";
        PriceCalendar priceCalendar = priceCalendarMapper.selectOne(
                new LambdaQueryWrapper<PriceCalendar>()
                        .eq(PriceCalendar::getSkuId, skuId)
                        .eq(PriceCalendar::getSaleDate, saleDate)
                        .eq(PriceCalendar::getPriceType, priceType));
        if (priceCalendar == null) {
            throw new BizException(PricingErrorCode.PRICE_NOT_CONFIGURED);
        }
        return PriceResponse.builder()
                .skuId(priceCalendar.getSkuId())
                .saleDate(priceCalendar.getSaleDate())
                .priceType(priceCalendar.getPriceType())
                .price(priceCalendar.getPrice())
                .build();
    }
}
