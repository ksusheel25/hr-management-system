package com.company.hrsystem.leave.controller;

import com.company.hrsystem.leave.dto.LeaveTypeCreateRequest;
import com.company.hrsystem.leave.dto.LeaveTypeDto;
import com.company.hrsystem.leave.dto.LeaveTypeUpdateRequest;
import com.company.hrsystem.leave.service.LeaveTypeService;
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
@RequestMapping("/api/v1/admin/leave-types")
@PreAuthorize("hasRole('HR')")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    @PostMapping
    public LeaveTypeDto create(@Valid @RequestBody LeaveTypeCreateRequest request) {
        return leaveTypeService.create(request);
    }

    @GetMapping
    public List<LeaveTypeDto> list() {
        return leaveTypeService.list();
    }

    @PutMapping("/{leaveTypeId}")
    public LeaveTypeDto update(@PathVariable UUID leaveTypeId, @Valid @RequestBody LeaveTypeUpdateRequest request) {
        return leaveTypeService.update(leaveTypeId, request);
    }

    @DeleteMapping("/{leaveTypeId}")
    public ResponseEntity<Void> delete(@PathVariable UUID leaveTypeId) {
        leaveTypeService.delete(leaveTypeId);
        return ResponseEntity.noContent().build();
    }
}
