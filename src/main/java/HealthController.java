package com.mysawit.mysawit_pembayaran;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final DbPingRepository repo;

    public HealthController(DbPingRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/db-ping")
    public Map<String, Object> dbPing(@RequestParam(defaultValue = "hello") String msg) {
        DbPing saved = repo.save(new DbPing(msg));
        return Map.of("status", "ok", "id", saved.getId(), "message", saved.getMessage());
    }

    @GetMapping("/db-ping/count")
    public Map<String, Object> count() {
        return Map.of("count", repo.count());
    }
}