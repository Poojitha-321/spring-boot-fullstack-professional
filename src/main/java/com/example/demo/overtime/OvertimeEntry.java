package com.example.demo.overtime;

import com.example.demo.attendance.AttendanceLog;
import com.example.demo.worker.Worker;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import javax.persistence.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "overtime_entry", indexes = {
    @Index(name = "idx_overtime_worker", columnList = "worker_id"),
    @Index(name = "idx_overtime_date", columnList = "date")
})
public class OvertimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = false)
    private AttendanceLog attendance;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Double overtimeHours;

    @Column(nullable = false)
    private Double overtimeRateApplied;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;
}
