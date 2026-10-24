package com.chandan.mpgloggerbackend.fuellog;

import com.chandan.mpgloggerbackend.user.AppUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's fuel logs. {@code AppUser user} is filled in from the session cookie (see user.WebConfig). */
@RestController
@RequestMapping("/api/fuel-logs")
public class FuelLogController {

    private final FuelLogService service;

    public FuelLogController(FuelLogService service) {
        this.service = service;
    }

    /** All of this user's logs, highest odometer first. */
    @GetMapping
    public List<FuelLogResponse> list(AppUser user) {
        return service.findAll(user);
    }

    @PostMapping
    public ResponseEntity<FuelLogResponse> create(AppUser user, @Valid @RequestBody FuelLogRequest request) {
        FuelLogResponse created = service.create(user, request);
        return ResponseEntity
                .created(URI.create("/api/fuel-logs/" + created.id()))
                .body(created);
    }

    /** Edits a log; if the odometer changes, the next log starts at the new reading. */
    @PutMapping("/{id}")
    public FuelLogResponse update(AppUser user, @PathVariable Long id, @Valid @RequestBody FuelLogRequest request) {
        return service.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(AppUser user, @PathVariable Long id) {
        service.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
