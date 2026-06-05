package com.example.demo.attendance;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/clock-in")
    public ResponseEntity<AttendanceLog> clockIn(@RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(attendanceService.clockIn(body.get("workerId"), body.get("siteId")));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<AttendanceLog> clockOut(@RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(attendanceService.clockOut(body.get("workerId")));
    }

    @GetMapping("/active")
    public ResponseEntity<Object> getActiveWorkers() {
        return ResponseEntity.ok(attendanceService.getActiveWorkers());
    }

    @GetMapping("/log")
    public ResponseEntity<Page<AttendanceLog>> getLog(
        @RequestParam Long workerId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(attendanceService.getAttendanceHistory(workerId, from, to, PageRequest.of(page, size)));
    }
}
