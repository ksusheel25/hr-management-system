package com.company.hrsystem.attendance.repository;

import com.company.hrsystem.attendance.entity.AttendanceEvent;
import com.company.hrsystem.attendance.entity.AttendanceEventType;
import com.company.hrsystem.attendance.entity.AttendanceSource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceEventRepository extends JpaRepository<AttendanceEvent, UUID> {

    @Query("""
            select ae from AttendanceEvent ae
            where ae.companyId = :companyId
              and ae.employee.id = :employeeId
              and ae.eventType = :eventType
            order by ae.eventTime desc
            """)
    List<AttendanceEvent> findLatestByEmployeeAndEventType(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("eventType") AttendanceEventType eventType,
            Pageable pageable);

    default Optional<AttendanceEvent> findTopByEmployeeIdAndEventTypeOrderByEventTimeDesc(
            UUID companyId,
            UUID employeeId,
            AttendanceEventType eventType) {
        return findLatestByEmployeeAndEventType(
                companyId,
                employeeId,
                eventType,
                PageRequest.of(0, 1)).stream().findFirst();
    }

    @Query("""
            select ci from AttendanceEvent ci
            where ci.companyId = :companyId
              and ci.employee.id = :employeeId
              and ci.eventType = :checkInType
              and not exists (
                select co.id from AttendanceEvent co
                where co.companyId = :companyId
                  and co.employee.id = :employeeId
                  and co.eventType = :checkOutType
                  and co.eventTime > ci.eventTime
              )
            order by ci.eventTime desc
            """)
    List<AttendanceEvent> findOpenSessionCandidates(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("checkInType") AttendanceEventType checkInType,
            @Param("checkOutType") AttendanceEventType checkOutType,
            Pageable pageable);

    default Optional<AttendanceEvent> findLatestOpenSession(UUID companyId, UUID employeeId) {
        return findOpenSessionCandidates(
                companyId,
                employeeId,
                AttendanceEventType.CHECK_IN,
                AttendanceEventType.CHECK_OUT,
                PageRequest.of(0, 1)).stream().findFirst();
    }

    @Query("""
            select distinct ae.employee.id from AttendanceEvent ae
            where ae.companyId = :companyId
              and ae.source = :source
              and ae.eventType = :eventType
              and ae.eventTime >= :fromTime
              and ae.eventTime < :toTime
            """)
    Set<UUID> findDistinctEmployeeIdsByCompanyAndSourceAndEventTypeAndEventTimeBetween(
            @Param("companyId") UUID companyId,
            @Param("source") AttendanceSource source,
            @Param("eventType") AttendanceEventType eventType,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime);

    @Query("""
            select ae from AttendanceEvent ae
            where ae.companyId = :companyId
              and ae.eventType in :eventTypes
              and ae.eventTime >= :fromTime
              and ae.eventTime < :toTime
            order by ae.eventTime asc
            """)
    List<AttendanceEvent> findByCompanyAndEventTypesAndEventTimeBetween(
            @Param("companyId") UUID companyId,
            @Param("eventTypes") Set<AttendanceEventType> eventTypes,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime);

    @Query("""
            select ae from AttendanceEvent ae
            where ae.companyId = :companyId
              and ae.employee.id = :employeeId
              and ae.source = :source
              and ae.eventType in :eventTypes
              and ae.eventTime >= :fromTime
              and ae.eventTime < :toTime
            order by ae.eventTime asc
            """)
    List<AttendanceEvent> findByCompanyAndEmployeeAndSourceAndEventTypesAndEventTimeBetween(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            @Param("source") AttendanceSource source,
            @Param("eventTypes") Set<AttendanceEventType> eventTypes,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime);
}
