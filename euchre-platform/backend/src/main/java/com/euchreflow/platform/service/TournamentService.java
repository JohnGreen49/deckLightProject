package com.euchreflow.platform.service;

import com.euchreflow.platform.domain.*;
import com.euchreflow.platform.repo.*;
import com.euchreflow.platform.scheduling.*;
import com.euchreflow.platform.web.NotFoundException;
import com.euchreflow.platform.web.dto.TournamentDtos.CreateTournament;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class TournamentService {

    private final TournamentRepository tournaments;
    private final EnrollmentRepository enrollments;
    private final RoundRepository rounds;
    private final SeatingRepository seatings;
    private final PlayerRepository players;
    private final GroupService groups;
    private final SchedulerRegistry schedulerRegistry;

    public TournamentService(TournamentRepository tournaments, EnrollmentRepository enrollments,
                             RoundRepository rounds, SeatingRepository seatings,
                             PlayerRepository players, GroupService groups,
                             SchedulerRegistry schedulerRegistry) {
        this.tournaments = tournaments;
        this.enrollments = enrollments;
        this.rounds = rounds;
        this.seatings = seatings;
        this.players = players;
        this.groups = groups;
        this.schedulerRegistry = schedulerRegistry;
    }

    @Transactional
    public Tournament create(Long groupId, CreateTournament req) {
        groups.get(groupId);
        Tournament t = new Tournament();
        t.setGroupId(groupId);
        t.setName(req.name());
        if (StringUtils.hasText(req.schedulerId())) {
            t.setSchedulerId(req.schedulerId());
        }
        return tournaments.save(t);
    }

    @Transactional(readOnly = true)
    public List<Tournament> listByGroup(Long groupId) {
        return tournaments.findByGroupIdOrderByCreatedAtDesc(groupId);
    }

    @Transactional(readOnly = true)
    public Tournament get(Long id) {
        return tournaments.findById(id).orElseThrow(() -> new NotFoundException("tournament not found: " + id));
    }

    /**
     * Enroll the given players (in order) and (re)generate the full schedule. Any previously
     * generated rounds, seatings, and scores for this tournament are discarded first.
     *
     * @return the persisted tournament, now ACTIVE with {@code numRounds} set
     */
    @Transactional
    public Tournament generateSchedule(Long tournamentId, List<Long> playerIds, Integer requestedRounds) {
        Tournament t = get(tournamentId);

        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(playerIds));
        if (distinctIds.size() != playerIds.size()) {
            throw new IllegalArgumentException("duplicate players in enrollment list");
        }
        // Validate every player exists and belongs to this tournament's group.
        for (Long pid : distinctIds) {
            Player p = players.findById(pid)
                    .orElseThrow(() -> new NotFoundException("player not found: " + pid));
            if (!p.getGroupId().equals(t.getGroupId())) {
                throw new IllegalArgumentException("player " + pid + " is not in this group");
            }
        }
        int n = distinctIds.size();

        clearExisting(tournamentId);

        // Enroll players and remember seat index -> player id.
        Long[] seatToPlayer = new Long[n];
        for (int i = 0; i < n; i++) {
            Enrollment e = new Enrollment();
            e.setTournamentId(tournamentId);
            e.setPlayerId(distinctIds.get(i));
            e.setSeatIndex(i);
            enrollments.save(e);
            seatToPlayer[i] = distinctIds.get(i);
        }

        TournamentScheduler scheduler = schedulerRegistry.resolve(t.getSchedulerId(), n);
        Schedule schedule = scheduler.build(new ScheduleRequest(n, requestedRounds));

        for (ScheduledRound sr : schedule.rounds()) {
            Round round = new Round();
            round.setTournamentId(tournamentId);
            round.setRoundNumber(sr.roundNumber());
            round = rounds.save(round);

            for (TableAssignment ta : sr.tables()) {
                Seating seating = new Seating();
                seating.setRoundId(round.getId());
                seating.setTableNumber(ta.tableNumber());
                seating.setTeam1p1(seatToPlayer[ta.t1a()]);
                seating.setTeam1p2(seatToPlayer[ta.t1b()]);
                seating.setTeam2p1(seatToPlayer[ta.t2a()]);
                seating.setTeam2p2(seatToPlayer[ta.t2b()]);
                seatings.save(seating);
            }
        }

        t.setNumRounds(schedule.rounds().size());
        t.setStatus(TournamentStatus.ACTIVE);
        return tournaments.save(t);
    }

    private void clearExisting(Long tournamentId) {
        // DB-level ON DELETE CASCADE removes seatings and game_scores when rounds are deleted.
        List<Round> existingRounds = rounds.findByTournamentIdOrderByRoundNumber(tournamentId);
        rounds.deleteAll(existingRounds);
        List<Enrollment> existingEnrollments = enrollments.findByTournamentIdOrderBySeatIndex(tournamentId);
        enrollments.deleteAll(existingEnrollments);
    }
}
