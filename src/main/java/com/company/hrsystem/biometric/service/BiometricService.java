package com.company.hrsystem.biometric.service;

import com.company.hrsystem.biometric.dto.BiometricEventRequest;
import com.company.hrsystem.biometric.entity.BiometricEventLog;
import com.company.hrsystem.biometric.event.BiometricEventReceived;
import com.company.hrsystem.biometric.repository.BiometricEventLogRepository;
import com.company.hrsystem.common.context.CompanyContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class BiometricService {

    private final BiometricEventLogRepository biometricEventLogRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void receiveEvent(BiometricEventRequest request) {
        var companyId = CompanyContext.getCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context is missing"));

        if (biometricEventLogRepository.existsByDeviceLogIdAndCompanyId(request.deviceLogId(), companyId)) {
            throw new IllegalStateException("Duplicate biometric event for deviceLogId");
        }

        var eventLog = new BiometricEventLog();
        eventLog.setCompanyId(companyId);
        eventLog.setDeviceId(request.deviceId());
        eventLog.setDeviceLogId(request.deviceLogId());
        eventLog.setEmployeeCode(request.employeeCode());
        eventLog.setEventType(request.eventType());
        eventLog.setEventTime(request.eventTime());
        eventLog.setProcessed(Boolean.FALSE);

        var savedEventLog = biometricEventLogRepository.save(eventLog);
        publishAfterCommit(new BiometricEventReceived(savedEventLog.getId(), companyId));
    }

    private void publishAfterCommit(BiometricEventReceived event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    applicationEventPublisher.publishEvent(event);
                }
            });
            return;
        }
        applicationEventPublisher.publishEvent(event);
    }
}
