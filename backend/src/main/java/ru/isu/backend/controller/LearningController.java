package ru.isu.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.isu.backend.dto.request.AnswerCardRequest;
import ru.isu.backend.dto.response.DailyStatsResponse;
import ru.isu.backend.dto.response.LeaderboardRowResponse;
import ru.isu.backend.dto.response.ProgressResponse;
import ru.isu.backend.dto.response.TrainingNextResponse;
import ru.isu.backend.model.LeaderboardPeriod;
import ru.isu.backend.model.TrainingQueueMode;
import ru.isu.backend.security.UserPrincipal;
import ru.isu.backend.service.LearningService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class LearningController {

    private final LearningService learningService;

    @GetMapping("/progress")
    public List<ProgressResponse> progress(@AuthenticationPrincipal UserPrincipal principal) {
        return learningService.getProgress(principal.getId());
    }

    @PostMapping("/progress/answers")
    public ProgressResponse answer(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AnswerCardRequest request
    ) {
        return learningService.answerCard(principal.getId(), request);
    }

    @GetMapping("/training/next")
    public TrainingNextResponse nextTrainingCard(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @Positive Long deckId,
            @RequestParam(defaultValue = "GOAL") TrainingQueueMode queueMode,
            @RequestParam(defaultValue = "0") Integer extraLimit,
            @RequestParam(defaultValue = "0") Integer extraNewLimit,
            @RequestParam(defaultValue = "0") Integer extraReviewLimit
    ) {
        return learningService.getNextTrainingCard(
                principal.getId(),
                deckId,
                queueMode,
                extraLimit,
                extraNewLimit,
                extraReviewLimit
        );
    }

    @GetMapping("/stats/daily")
    public DailyStatsResponse dailyStats(@AuthenticationPrincipal UserPrincipal principal) {
        return learningService.getDailyStats(principal.getId());
    }

    @GetMapping("/stats/daily/history")
    public List<DailyStatsResponse> dailyStatsHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "7") Integer days
    ) {
        return learningService.getDailyStatsHistory(principal.getId(), days);
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardRowResponse> leaderboard(
            @RequestParam(defaultValue = "DAY") LeaderboardPeriod period
    ) {
        return learningService.getLeaderboard(period);
    }
}
