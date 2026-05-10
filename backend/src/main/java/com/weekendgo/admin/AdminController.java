package com.weekendgo.admin;

import com.weekendgo.common.api.ApiResponse;
import com.weekendgo.common.api.PageResult;
import com.weekendgo.interaction.AuditStats;
import com.weekendgo.interaction.InteractionRepository;
import com.weekendgo.interaction.PendingAuditItem;
import com.weekendgo.profile.WorkspaceProfileRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AdminController {

    private final InteractionRepository interactionRepository;
    private final WorkspaceProfileRepository workspaceProfileRepository;

    public AdminController(
            InteractionRepository interactionRepository,
            WorkspaceProfileRepository workspaceProfileRepository
    ) {
        this.interactionRepository = interactionRepository;
        this.workspaceProfileRepository = workspaceProfileRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/audits/pending-list")
    public ApiResponse<PageResult<PendingAuditItem>> pendingList(
            @RequestParam String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<PendingAuditItem> items;
        long total;
        switch (type) {
            case "profile" -> {
                items = workspaceProfileRepository.findPendingProfileSubmissions(page, size);
                total = workspaceProfileRepository.countPendingProfileSubmissions();
            }
            case "review" -> {
                items = interactionRepository.findPendingReviews(page, size);
                total = interactionRepository.countPendingReviews();
            }
            case "image" -> {
                items = interactionRepository.findPendingImages(page, size);
                total = interactionRepository.countPendingImages();
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid type: " + type);
        }
        return ApiResponse.ok(new PageResult<>(items, total, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/audits/stats")
    public ApiResponse<AuditStats> stats() {
        long pendingProfiles = workspaceProfileRepository.countPendingProfileSubmissions();
        long pendingReviews = interactionRepository.countPendingReviews();
        long pendingImages = interactionRepository.countPendingImages();
        long todayApproved = interactionRepository.countTodayApproved();
        long todayRejected = interactionRepository.countTodayRejected();
        return ApiResponse.ok(new AuditStats(pendingProfiles, pendingReviews, pendingImages, todayApproved, todayRejected));
    }
}
