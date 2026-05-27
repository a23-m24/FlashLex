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
import ru.isu.backend.dto.response.LearningStatsResponse;
import ru.isu.backend.dto.response.ProgressResponse;
import ru.isu.backend.dto.response.TrainingNextResponse;
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
            @RequestParam @Positive Long deckId
    ) {
        return learningService.getNextTrainingCard(principal.getId(), deckId);
    }

    @GetMapping("/stats/daily")
    public DailyStatsResponse dailyStats(@AuthenticationPrincipal UserPrincipal principal) {
        return learningService.getDailyStats(principal.getId());
    }

    @GetMapping("/stats/learning")
    public LearningStatsResponse learningStats(@AuthenticationPrincipal UserPrincipal principal) {
        return learningService.getLearningStats(principal.getId());
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardRowResponse> leaderboard() {
        return learningService.getLeaderboard();
    }
}
