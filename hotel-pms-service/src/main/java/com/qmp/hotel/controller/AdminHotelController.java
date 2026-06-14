package com.qmp.hotel.controller;

import com.qmp.hotel.dto.admin.CreateRoomTypeRequest;
import com.qmp.hotel.dto.admin.UpsertRoomBucketRequest;
import com.qmp.hotel.dto.UpdateStatusRequest;
import com.qmp.hotel.service.AdminHotelService;
import com.qmp.kernel.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 酒店后台管理接口（{@code /admin/v1}）。
 */
@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminHotelController {

    private final AdminHotelService adminHotelService;

    @PostMapping("/room-types")
    public ApiResponse<Map<String, Object>> createRoomType(@RequestBody CreateRoomTypeRequest request) {
        return ApiResponse.ok(Map.of("room_type_id", adminHotelService.createRoomType(request)));
    }

    @PatchMapping("/room-types/{roomTypeId}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long roomTypeId,
                                          @RequestBody UpdateStatusRequest request) {
        adminHotelService.updateStatus(roomTypeId, request.getStatus());
        return ApiResponse.ok();
    }

    @PostMapping("/room-buckets")
    public ApiResponse<Map<String, Object>> upsertBuckets(@RequestBody UpsertRoomBucketRequest request) {
        return ApiResponse.ok(Map.of("nights", adminHotelService.upsertBuckets(request)));
    }
}
