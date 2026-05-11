package com.weekendgo.profile;

import com.weekendgo.interaction.InteractionRepository;
import com.weekendgo.interaction.ReviewResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryWorkspaceProfileRepository implements WorkspaceProfileRepository {

    private final InteractionRepository interactionRepository;

    public InMemoryWorkspaceProfileRepository(InteractionRepository interactionRepository) {
        this.interactionRepository = interactionRepository;
    }

    @Override
    public Optional<WorkspaceProfile> findProfileByPlaceId(long placeId) {
        List<ReviewResponse> approved = interactionRepository.findApprovedReviews(placeId);
        if (approved.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(buildProfile(placeId, approved));
    }

    private WorkspaceProfile buildProfile(long placeId, List<ReviewResponse> approved) {
        return new WorkspaceProfile(
                placeId,
                average(approved.stream().map(ReviewResponse::quietScore).toList(), 1),
                average(approved.stream().map(ReviewResponse::wifiScore).toList(), 1),
                average(approved.stream().map(ReviewResponse::socketScore).toList(), 1),
                average(approved.stream().map(ReviewResponse::seatScore).toList(), 1),
                average(approved.stream().map(ReviewResponse::costScore).toList(), 1),
                approved.stream().map(ReviewResponse::minConsumption).filter(v -> v != null).min(Integer::compareTo).orElse(null),
                aggregateAllowLongStay(approved),
                profileScore(
                        average(approved.stream().map(ReviewResponse::quietScore).toList(), 2),
                        average(approved.stream().map(ReviewResponse::wifiScore).toList(), 2),
                        average(approved.stream().map(ReviewResponse::socketScore).toList(), 2),
                        average(approved.stream().map(ReviewResponse::seatScore).toList(), 2),
                        average(approved.stream().map(ReviewResponse::costScore).toList(), 2)
                ),
                trustLevel(approved.size()),
                approved.size(),
                (int) approved.stream().map(ReviewResponse::userId).distinct().count(),
                approved.stream().map(ReviewResponse::createdAt).max(Instant::compareTo).orElse(null)
        );
    }

    private BigDecimal average(List<BigDecimal> values, int scale) {
        List<BigDecimal> present = new ArrayList<>();
        for (BigDecimal value : values) {
            if (value != null) {
                present.add(value);
            }
        }
        if (present.isEmpty()) {
            return null;
        }
        BigDecimal sum = present.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(present.size()), scale, RoundingMode.HALF_UP);
    }

    private BigDecimal profileScore(BigDecimal... values) {
        List<BigDecimal> present = new ArrayList<>();
        for (BigDecimal value : values) {
            if (value != null) {
                present.add(value);
            }
        }
        if (present.isEmpty()) {
            return null;
        }
        BigDecimal sum = present.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(present.size()), 2, RoundingMode.HALF_UP);
    }

    private AllowLongStay aggregateAllowLongStay(List<ReviewResponse> approved) {
        int trueCount = 0;
        int falseCount = 0;
        for (ReviewResponse review : approved) {
            if (review.allowLongStay() == AllowLongStay.TRUE) {
                trueCount++;
            } else if (review.allowLongStay() == AllowLongStay.FALSE) {
                falseCount++;
            }
        }
        if (trueCount == 0 && falseCount == 0) {
            return AllowLongStay.UNKNOWN;
        }
        return trueCount >= falseCount ? AllowLongStay.TRUE : AllowLongStay.FALSE;
    }

    private TrustLevel trustLevel(int approvedSubmissionCount) {
        if (approvedSubmissionCount >= 10) {
            return TrustLevel.HIGH;
        }
        if (approvedSubmissionCount >= 3) {
            return TrustLevel.MEDIUM;
        }
        return TrustLevel.LOW;
    }
}
