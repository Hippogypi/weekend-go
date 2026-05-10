package com.weekendgo.checkin;

import com.weekendgo.place.PlaceNotFoundException;
import com.weekendgo.place.PlaceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CheckinService {

    private static final Duration RECENT_WINDOW = Duration.ofHours(2);

    private final CheckinRepository checkinRepository;
    private final PlaceRepository placeRepository;
    private final Clock clock;

    public CheckinService(CheckinRepository checkinRepository, PlaceRepository placeRepository, Clock clock) {
        this.checkinRepository = checkinRepository;
        this.placeRepository = placeRepository;
        this.clock = clock;
    }

    public CheckinResponse create(long placeId, long userId, CheckinRequest request) {
        ensurePlaceExists(placeId);
        SavedCheckin saved = checkinRepository.save(new NewCheckin(
                placeId,
                userId,
                request.crowdLevel(),
                request.noiseLevel(),
                request.hasSeat(),
                request.remark(),
                Instant.now(clock)
        ));
        return CheckinResponse.from(saved);
    }

    public CurrentStatusResponse currentStatus(long placeId) {
        ensurePlaceExists(placeId);
        Instant cutoff = Instant.now(clock).minus(RECENT_WINDOW);
        List<SavedCheckin> recentCheckins = checkinRepository.findRecentByPlaceId(placeId, cutoff);
        if (recentCheckins.isEmpty()) {
            return CurrentStatusResponse.empty(placeId, cutoff);
        }

        int seatCount = (int) recentCheckins.stream().filter(SavedCheckin::hasSeat).count();
        BigDecimal seatAvailabilityRatio = BigDecimal.valueOf(seatCount)
                .divide(BigDecimal.valueOf(recentCheckins.size()), 2, RoundingMode.HALF_UP);

        return CurrentStatusResponse.active(
                placeId,
                cutoff,
                recentCheckins.size(),
                mostFrequent(recentCheckins, SavedCheckin::crowdLevel, CrowdLevel.class),
                mostFrequent(recentCheckins, SavedCheckin::noiseLevel, NoiseLevel.class),
                seatCount * 2 >= recentCheckins.size(),
                seatAvailabilityRatio
        );
    }

    public List<MyCheckinResponse> myCheckins(long userId) {
        return checkinRepository.findByUserId(userId).stream()
                .map(checkin -> {
                    String placeName = placeRepository.findById(checkin.placeId())
                            .map(com.weekendgo.place.Place::name)
                            .orElse("未知地点");
                    return new MyCheckinResponse(
                            checkin.id(),
                            checkin.placeId(),
                            placeName,
                            checkin.userId(),
                            checkin.crowdLevel(),
                            checkin.noiseLevel(),
                            checkin.hasSeat(),
                            checkin.remark(),
                            checkin.createdAt()
                    );
                })
                .toList();
    }

    private void ensurePlaceExists(long placeId) {
        if (placeRepository.findById(placeId).isEmpty()) {
            throw new PlaceNotFoundException();
        }
    }

    private <T extends Enum<T>> T mostFrequent(List<SavedCheckin> checkins, Function<SavedCheckin, T> mapper, Class<T> type) {
        Map<T, Long> counts = checkins.stream()
                .collect(Collectors.groupingBy(mapper, Collectors.counting()));
        return counts.entrySet().stream()
                .max(Comparator
                        .comparing(Map.Entry<T, Long>::getValue)
                        .thenComparing(entry -> -entry.getKey().ordinal()))
                .map(Map.Entry::getKey)
                .orElseThrow();
    }
}
