package com.company.hrsystem.leave.controller;

import com.company.hrsystem.leave.dto.BulkUploadResultDto;
import com.company.hrsystem.leave.service.EmployeeBulkUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserBulkUploadController {

    private final EmployeeBulkUploadService employeeBulkUploadService;

    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('HR')")
    public BulkUploadResultDto bulkUpload(@RequestParam("file") MultipartFile file) {
        return employeeBulkUploadService.bulkUpload(file);
    }
}
