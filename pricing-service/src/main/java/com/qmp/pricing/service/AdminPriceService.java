package com.qmp.pricing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.pricing.dto.PriceResponse;
import com.qmp.pricing.dto.admin.UpsertPriceRequest;
import com.qmp.pricing.entity.PriceCalendar;
import com.qmp.pricing.mapper.PriceCalendarMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 价格中心后台管理服务：按 (sku_id, sale_date, price_type) 幂等 upsert 价格、按 sku+日期查询。
 * 让后台维护门市价/会员价，替代 Flyway 种子（见 pricing-service CLAUDE.md「后台管理」）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPriceService {

    private final PriceCalendarMapper priceCalendarMapper;

    public Long upsertPrice(UpsertPriceRequest request) {
        PriceCalendar existing = priceCalendarMapper.selectOne(new LambdaQueryWrapper<PriceCalendar>()
                .eq(PriceCalendar::getSkuId, request.getSkuId())
                .eq(PriceCalendar::getSaleDate, request.getSaleDate())
                .eq(PriceCalendar::getPriceType, request.getPriceType()));
        if (existing != null) {
            existing.setPrice(request.getPrice());
            priceCalendarMapper.updateById(existing);
            log.info("后台更新价格: skuId={}, date={}, type={}, price={}",
                    request.getSkuId(), request.getSaleDate(), request.getPriceType(), request.getPrice());
            return existing.getPriceCalendarId();
        }
        PriceCalendar pc = new PriceCalendar();
        pc.setTenantId(TenantContext.get());
        pc.setSkuId(request.getSkuId());
        pc.setSaleDate(request.getSaleDate());
        pc.setPriceType(request.getPriceType());
        pc.setPrice(request.getPrice());
        priceCalendarMapper.insert(pc);
        log.info("后台新增价格: skuId={}, date={}, type={}, price={}",
                request.getSkuId(), request.getSaleDate(), request.getPriceType(), request.getPrice());
        return pc.getPriceCalendarId();
    }

    public List<PriceResponse> listPrices(Long skuId, LocalDate saleDate) {
        LambdaQueryWrapper<PriceCalendar> wrapper = new LambdaQueryWrapper<PriceCalendar>()
                .eq(PriceCalendar::getSkuId, skuId);
        if (saleDate != null) {
            wrapper.eq(PriceCalendar::getSaleDate, saleDate);
        }
        return priceCalendarMapper.selectList(wrapper.orderByAsc(PriceCalendar::getSaleDate)).stream()
                .map(pc -> PriceResponse.builder()
                        .skuId(pc.getSkuId())
                        .saleDate(pc.getSaleDate())
                        .priceType(pc.getPriceType())
                        .price(pc.getPrice())
                        .build())
                .toList();
    }
}
