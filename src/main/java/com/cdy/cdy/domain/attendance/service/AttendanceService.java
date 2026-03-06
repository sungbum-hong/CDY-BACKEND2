package com.cdy.cdy.domain.attendance.service;

import com.cdy.cdy.domain.attendance.dto.CalendarDay;
import com.cdy.cdy.domain.attendance.entity.DailyAttendance;
import com.cdy.cdy.domain.attendance.repository.AttendanceRepository;
import com.cdy.cdy.domain.users.entity.Users;
import com.cdy.cdy.domain.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import com.cdy.cdy.domain.attendance.dto.MonthCalendarResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Service
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;



    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     출석체크 롲기
     이미 출석체크를 했으면 (existsByUser_IndAndCheckDate) < 예외발생
     */
    public void checkToday(String username) {

        Users users = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("유저정보를 찾을 수 없습니다."));

        LocalDate today = LocalDate.now(KST);
        if (attendanceRepository.existsByUser_IdAndCheckDate(users.getId(), today)) {
            throw new IllegalArgumentException("이미 출석체크를 등록하였습니다.");
        }

        try {

            DailyAttendance att = DailyAttendance.check(
                    users,
                    today,
                    LocalDateTime.now(KST)
            );
            attendanceRepository.saveAndFlush(att); // 바로 flush해서 UNIQUE 위반 즉시 감지
        } catch (DataIntegrityViolationException ignore) {
            // 동시 클릭/중복요청 → 이미 누가 먼저 저장함 → 무시(멱등)
        }
    }

    /** 2) 월 달력 조회: 날짜별 checked만 넘김 */
    @Transactional(readOnly = true)
    public MonthCalendarResponse getMonth(String username, YearMonth ym) {
        // ① 이번 달 범위(1일~말일) 확정
        LocalDate start = ym.atDay(1);        // ex) 2025-09-01
        LocalDate end   = ym.atEndOfMonth();  // ex) 2025-09-30


        Users users = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 유저"));

        // ② DB에서 '이번 달에 찍힌 출석' 전부 조회 (Between = 양 끝 포함)
        List<DailyAttendance> attendancesThisMonth =
                attendanceRepository.findAllByUser_IdAndCheckDateBetween(users.getId(), start, end);

        // ③ 날짜만 뽑아서 Set으로 (O(1) 포함 체크용)
        Set<LocalDate> checkedDates = attendancesThisMonth.stream()
                .map(DailyAttendance::getCheckDate)  // 엔티티 → 날짜
                .collect(Collectors.toSet());

        // (이 아래에서 checkedDates.contains(d) 로 true/false 칠하면 됨)
        List<CalendarDay> days = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            days.add(CalendarDay.builder()
                    .date(d)
                    .checked(checkedDates.contains(d))
                    .build());
        }

        return MonthCalendarResponse.builder()
                .month(ym.toString()) // "YYYY-MM" (DTO가 String month일 때)
                .days(days)
                .build();
    }

    /**
     *
     * 오늘 유저가 출석체크를 했는지 확인하는로직
     * 시간대는 한국 시간대 기준
     */
    public Boolean isCheckedToday(String username) {

        Users users = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));


        boolean result = attendanceRepository.existsByUser_IdAndCheckDate
                (users.getId(), LocalDate.now(ZoneId.of("Asia/Seoul")));

        return result;

    }
    }
