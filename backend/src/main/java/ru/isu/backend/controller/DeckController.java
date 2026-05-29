package ru.isu.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.isu.backend.dto.request.DeckRequest;
import ru.isu.backend.dto.request.RatingRequest;
import ru.isu.backend.dto.response.DeckCatalogFacetsResponse;
import ru.isu.backend.dto.response.DeckResponse;
import ru.isu.backend.security.UserPrincipal;
import ru.isu.backend.service.DeckService;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;

    @GetMapping("/my")
    public List<DeckResponse> getMyDecks(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return deckService.getMyDecks(principal.getId());
    }

    @GetMapping("/public")
    public Page<DeckResponse> searchPublishedDecks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String tag,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = principal == null ? null : principal.getId();
        return deckService.searchPublishedDecks(query, level, tag, userId, pageable);
    }

    @GetMapping("/public/facets")
    public DeckCatalogFacetsResponse getPublishedFacets() {
        return deckService.getPublishedFacets();
    }

    @GetMapping("/{deckId}")
    public DeckResponse getDeck(
            @PathVariable @Positive Long deckId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal == null ? null : principal.getId();
        return deckService.getDeck(deckId, userId, principal == null ? null : principal.getRole());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeckResponse createDeck(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeckRequest request
    ) {
        return deckService.createDeck(principal.getId(), request);
    }

    @PutMapping("/{deckId}")
    public DeckResponse updateDeck(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long deckId,
            @Valid @RequestBody DeckRequest request
    ) {
        return deckService.updateDeck(principal.getId(), deckId, request);
    }

    @PostMapping("/{deckId}/publish")
    public DeckResponse publishDeck(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long deckId
    ) {
        return deckService.publishDeck(principal.getId(), deckId);
    }

    @PostMapping("/{deckId}/clone")
    @ResponseStatus(HttpStatus.CREATED)
    public DeckResponse cloneDeck(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long deckId
    ) {
        return deckService.cloneDeck(principal.getId(), deckId);
    }

    @PostMapping("/{deckId}/rating")
    public DeckResponse rateDeck(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long deckId,
            @Valid @RequestBody RatingRequest request
    ) {
        return deckService.rateDeck(principal.getId(), deckId, request);
    }

    @DeleteMapping("/{deckId}/rating")
    public DeckResponse removeRating(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long deckId
    ) {
        return deckService.removeRating(principal.getId(), deckId);
    }

    @DeleteMapping("/{deckId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeck(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long deckId
    ) {
        deckService.deleteDeck(principal.getId(), deckId);
    }
}
