package com.ksh.companybackend.config;

import com.ksh.companybackend.game.application.RoomService;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// POST /join 으로 자리는 잡혔는데 소켓 enter 가 오지 않는 경우가 있다 - 그 사이에 탭을 닫으면
// 그렇다. 끊김 이벤트도 오지 않으므로(연결된 적이 없다) 시간으로만 걷어낼 수 있다.
@Component
public class SeatReclaimScheduler {

    private static final Duration ENTER_GRACE = Duration.ofSeconds(30);

    private final RoomService roomService;

    public SeatReclaimScheduler(RoomService roomService) {
        this.roomService = roomService;
    }

    @Scheduled(fixedDelay = 10_000)
    public void reclaim() {
        roomService.reclaimSeatsAbandonedBefore(Instant.now().minus(ENTER_GRACE));
    }
}
