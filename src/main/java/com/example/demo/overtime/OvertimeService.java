package com.example.demo.overtime;

import com.example.demo.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OvertimeService {

    private final OvertimeRepository overtimeRepository;

    public Map<String, Object> getMonthlySummary(Long workerId, String month) {
        YearMonth ym = YearMonth.parse(month);
        List<OvertimeEntry> entries = overtimeRepository.findByWorkerIdAndMonth(workerId, ym.getYear(), ym.getMonthValue());
        double totalHours = entries.stream().mapToDouble(OvertimeEntry::getOvertimeHours).sum();
        double totalAmount = entries.stream().mapToDouble(OvertimeEntry::getAmount).sum();
        return Map.of(
            "workerId", workerId,
            "month", month,
            "totalOvertimeHours", totalHours,
            "totalAmount", totalAmount,
            "entries", entries,
            "settlementStatus", entries.stream().allMatch(e -> e.getSettlementStatus() == SettlementStatus.SETTLED) ? "SETTLED" : "PENDING"
        );
    }

    @Transactional
    public Map<String, Object> settleOvertime(Long workerId, String month) {
        YearMonth ym = YearMonth.parse(month);

        // Cannot settle current month
        if (ym.equals(YearMonth.now()))
            throw new AppException("INVALID_SETTLEMENT", "Cannot settle the current month", HttpStatus.BAD_REQUEST);

        List<OvertimeEntry> entries = overtimeRepository.findByWorkerIdAndMonth(workerId, ym.getYear(), ym.getMonthValue());

        if (entries.isEmpty())
            throw new AppException("NO_ENTRIES", "No overtime entries found for this worker and month", HttpStatus.NOT_FOUND);

        if (entries.stream().allMatch(e -> e.getSettlementStatus() == SettlementStatus.SETTLED))
            throw new AppException("ALREADY_SETTLED", "Overtime for this month is already settled", HttpStatus.CONFLICT);

        double totalAmount = entries.stream().mapToDouble(OvertimeEntry::getAmount).sum();

        entries.forEach(e -> e.setSettlementStatus(SettlementStatus.SETTLED));
        overtimeRepository.saveAll(entries);

        return Map.of(
            "workerId", workerId,
            "month", month,
            "settledEntries", entries.size(),
            "totalAmount", totalAmount,
            "status", "SETTLED"
        );
    }
}
