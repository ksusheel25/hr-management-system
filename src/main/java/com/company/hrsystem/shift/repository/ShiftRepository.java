package com.company.hrsystem.shift.repository;

import com.company.hrsystem.shift.entity.Shift;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<Shift, UUID> {
}
