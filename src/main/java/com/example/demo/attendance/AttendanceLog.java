package com.example.demo.attendance;

import com.example.demo.site.Site;
import com.example.demo.worker.Worker;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance_log", indexes = {
    @Index(name = "idx_attendance_worker", columnList = "worker_id"),
    @Index(name = "idx_attendance_clockin", columnList = "clock_in_time")
})
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "clock_in_time", nullable = false)
    private LocalDateTime clockInTime;

    @Column(name = "clock_out_time")
    private LocalDateTime clockOutTime;

    @Column(name = "total_hours")
    private Double totalHours;

    @Column(name = "overtime_hours")
    private Double overtimeHours;

    @Column(nullable = false)
    private Boolean flagged = false;
}
