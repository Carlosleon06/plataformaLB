package com.leonbon.tournaments;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface TournamentEntryRepository extends MongoRepository<TournamentEntry, String> {
    Optional<TournamentEntry> findByTournamentIdAndTeamId(String tournamentId, String teamId);

    Optional<TournamentEntry> findByTournamentIdAndPlayerId(String tournamentId, String playerId);

    List<TournamentEntry> findByTournamentIdOrderByCreatedAtAsc(String tournamentId);

    List<TournamentEntry> findByTournamentIdAndStatusOrderByCreatedAtAsc(String tournamentId, TournamentEntryStatus status);

    List<TournamentEntry> findByTeamIdAndStatusIn(String teamId, List<TournamentEntryStatus> statuses);

    List<TournamentEntry> findByPlayerIdAndStatusIn(String playerId, List<TournamentEntryStatus> statuses);

    @Query("{ 'selectedRosterUserIds': ?0, 'status': { $in: ?1 } }")
    List<TournamentEntry> findByRosterUserIdAndStatusIn(String rosterUserId, List<TournamentEntryStatus> statuses);
}
