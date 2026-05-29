package ru.isu.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.isu.backend.dto.request.DeckRequest;
import ru.isu.backend.dto.request.UserPublicationBanRequest;
import ru.isu.backend.dto.response.AdminDashboardResponse;
import ru.isu.backend.dto.response.AdminDeckResponse;
import ru.isu.backend.dto.response.AdminUserResponse;
import ru.isu.backend.dto.response.DeckResponse;
import ru.isu.backend.security.UserPrincipal;
import ru.isu.backend.service.AdminService;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/users")
    public Page<AdminUserResponse> users(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 10, sort = "registeredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return adminService.searchUsers(query, pageable);
    }

    @PatchMapping("/users/{userId}/publication-ban")
    public AdminUserResponse updatePublicationBan(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UserPublicationBanRequest request
    ) {
        return adminService.updatePublicationBan(principal.getId(), userId, request);
    }

    @GetMapping("/decks")
    public Page<AdminDeckResponse> decks(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return adminService.searchDecks(query, pageable);
    }

    @PutMapping("/decks/{deckId}")
    public DeckResponse updateDeck(
            @PathVariable @Positive Long deckId,
            @Valid @RequestBody DeckRequest request
    ) {
        return adminService.updatePublishedDeck(deckId, request);
    }

    @DeleteMapping("/decks/{deckId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeck(@PathVariable @Positive Long deckId) {
        adminService.deleteDeck(deckId);
    }
}
