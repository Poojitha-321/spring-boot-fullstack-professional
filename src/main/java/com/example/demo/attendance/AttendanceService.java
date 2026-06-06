package com.example.demo.attendance;

import com.example.demo.exception.AppException;
import com.example.demo.overtime.OvertimeEntry;
import com.example.demo.overtime.OvertimeRepository;
import com.example.demo.overtime.SettlementStatus;
import com.example.demo.site.Site;
import com.example.demo.site.SiteRepository;
import com.example.demo.worker.Worker;
import com.example.demo.worker.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final WorkerRepository workerRepository;
    private final SiteRepository siteRepository;
    private final OvertimeRepository overtimeRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REDIS_KEY = "active_workers";
    private static final double STANDARD_HOURS = 8.0;
    private static final double MONTHLY_OT_CAP = 60.0;

    @Transactional
    public AttendanceLog clockIn(Long workerId, Long siteId) {
        Worker worker = workerRepository.findById(workerId)
            .orElseThrow(() -> new AppException("WORKER_NOT_FOUND", "Worker not found with id: " + workerId, HttpStatus.NOT_FOUND));

        if (!worker.getActive())
            throw new AppException("WORKER_INACTIVE", "Worker is not active", HttpStatus.BAD_REQUEST);

        Site site = siteRepository.findById(siteId)
            .orElseThrow(() -> new AppException("SITE_NOT_FOUND", "Site not found with id: " + siteId, HttpStatus.NOT_FOUND));

        if (!site.getActive())
            throw new AppException("SITE_INACTIVE", "Site is not active", HttpStatus.BAD_REQUEST);

        attendanceRepository.findActiveByWorkerId(workerId).ifPresent(a -> {
            throw new AppException("DUPLICATE_CLOCK_IN",
                "Worker is already clocked in at Site: " + a.getSite().getSiteName(), HttpStatus.CONFLICT);
        });

        AttendanceLog log = new AttendanceLog();
        log.setWorker(worker);
        log.setSite(site);
        log.setClockInTime(LocalDateTime.now());
        log.setFlagged(false);
        AttendanceLog saved = attendanceRepository.save(log);

        // Add to Redis with 16 hour TTL
        String redisField = workerId.toString();
        String redisValue = workerId + "|" + site.getSiteName() + "|" + log.getClockInTime().toString();
        redisTemplate.opsForHash().put(REDIS_KEY, redisField, redisValue);
        redisTemplate.expire(REDIS_KEY, 16, TimeUnit.HOURS);

        return saved;
    }

    @Transactional
    public AttendanceLog clockOut(Long workerId) {
        AttendanceLog log = attendanceRepository.findActiveByWorkerId(workerId)
            .orElseThrow(() -> new AppException("NOT_CLOCKED_IN", "Worker is not currently clocked in", HttpStatus.BAD_REQUEST));

        LocalDateTime clockOut = LocalDateTime.now();
        log.setClockOutTime(clockOut);

        double totalHours = Duration.between(log.getClockInTime(), clockOut).toMinutes() / 60.0;
        log.setTotalHours(totalHours);

        // Flag if shift exceeds 16 hours
        if (totalHours > 16) log.setFlagged(true);

        // Calculate overtime
        double overtimeHours = 0;
        if (totalHours > STANDARD_HOURS) {
            overtimeHours = totalHours - STANDARD_HOURS;

            // Check monthly cap
            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();
            Double usedOT = overtimeRepository.sumOvertimeHoursByWorkerAndMonth(workerId, year, month);
            double currentUsed =
                  usedOT == null ? 0.0 : usedOT;

               double remaining =
               Math.max(0,
                MONTHLY_OT_CAP - currentUsed);
            double cappedOT = Math.min(overtimeHours, remaining);

            log.setOvertimeHours(cappedOT);

            if (cappedOT > 0) {
                // Calculate amount: first 2 hours at 1.5x, beyond at 2x
                double dailyRate = log.getWorker().getDailyWageRate();
                double hourlyRate = dailyRate / STANDARD_HOURS;
                double amount;
                if (cappedOT <= 2) {
                    amount = cappedOT * hourlyRate * 1.5;
                } else {
                    amount = (2 * hourlyRate * 1.5) + ((cappedOT - 2) * hourlyRate * 2);
                }

                OvertimeEntry ot = new OvertimeEntry();
                ot.setWorker(log.getWorker());
                ot.setAttendance(log);
                ot.setDate(LocalDate.now());
                ot.setOvertimeHours(cappedOT);
                ot.setOvertimeRateApplied(cappedOT <= 2 ? 1.5 : 2.0);
                ot.setAmount(amount);
                ot.setSettlementStatus(SettlementStatus.PENDING);
                overtimeRepository.save(ot);
            }
        } else {
            log.setOvertimeHours(0.0);
        }

        // Remove from Redis
        redisTemplate.opsForHash().delete(REDIS_KEY, workerId.toString());

        return attendanceRepository.save(log);
    }

    public Object getActiveWorkers() {
        return redisTemplate.opsForHash().entries(REDIS_KEY);
    }

    public Page<AttendanceLog> getAttendanceHistory(Long workerId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return attendanceRepository.findByWorkerIdAndDateRange(workerId, from, to, pageable);
    }
}
