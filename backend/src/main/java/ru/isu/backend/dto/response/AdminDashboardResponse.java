package ru.isu.backend.dto.response;

public record AdminDashboardResponse(
        long users,
        long students,
        long admins,
        long publicationBannedUsers,
        long decks,
        long publishedDecks,
        long privateDecks,
        long flashcards,
        long ratings
) {
}
