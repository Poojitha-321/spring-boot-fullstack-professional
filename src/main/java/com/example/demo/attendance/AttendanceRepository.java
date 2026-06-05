package com.example.demo.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceLog, Long> {

    @EntityGraph(attributePaths = {"worker", "site"})
    @Query("""
        SELECT a
        FROM AttendanceLog a
        WHERE a.worker.id = :workerId
        AND a.clockOutTime IS NULL
    """)
    Optional<AttendanceLog> findActiveByWorkerId(
            @Param("workerId") Long workerId
    );

    @EntityGraph(attributePaths = {"worker", "site"})
    @Query("""
        SELECT a
        FROM AttendanceLog a
        WHERE a.worker.id = :workerId
        AND a.clockInTime BETWEEN :from AND :to
    """)
    Page<AttendanceLog> findByWorkerIdAndDateRange(
            @Param("workerId") Long workerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}