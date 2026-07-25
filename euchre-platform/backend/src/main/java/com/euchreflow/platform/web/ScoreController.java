package com.euchreflow.platform.web;

import com.euchreflow.platform.domain.GameScore;
import com.euchreflow.platform.service.ScoreService;
import com.euchreflow.platform.web.dto.ScoreDtos.ScoreView;
import com.euchreflow.platform.web.dto.ScoreDtos.SubmitScore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seatings")
public class ScoreController {

    private final ScoreService scores;

    public ScoreController(ScoreService scores) {
        this.scores = scores;
    }

    /** Submit or correct the result of a seating; partner pages and the leaderboard update live. */
    @PostMapping("/{seatingId}/score")
    public ScoreView submit(@PathVariable Long seatingId, @Valid @RequestBody SubmitScore req) {
        GameScore saved = scores.submit(seatingId, req);
        return new ScoreView(saved.getSeatingId(), saved.getTeam1Score(), saved.getTeam2Score());
    }
}
