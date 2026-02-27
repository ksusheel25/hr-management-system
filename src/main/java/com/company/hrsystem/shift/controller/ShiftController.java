package com.company.hrsystem.shift.controller;

import com.company.hrsystem.shift.dto.ShiftCreateRequest;
import com.company.hrsystem.shift.dto.ShiftDto;
import com.company.hrsystem.shift.dto.ShiftUpdateRequest;
import com.company.hrsystem.shift.service.ShiftService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/shifts")
@PreAuthorize("hasRole('HR')")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @PostMapping
    public ShiftDto createShift(@Valid @RequestBody ShiftCreateRequest request) {
        return shiftService.createShift(request);
    }

    @GetMapping
    public List<ShiftDto> listShifts() {
        return shiftService.listShifts();
    }

    @PutMapping("/{shiftId}")
    public ShiftDto updateShift(@PathVariable UUID shiftId, @Valid @RequestBody ShiftUpdateRequest request) {
        return shiftService.updateShift(shiftId, request);
    }

    @DeleteMapping("/{shiftId}")
    public ResponseEntity<Void> deleteShift(@PathVariable UUID shiftId) {
        shiftService.deleteShift(shiftId);
        return ResponseEntity.noContent().build();
    }
}
