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

    /** @param service does the work; this class only maps HTTP to it */
    public FuelLogController(FuelLogService service) {
        this.service = service;
    }

    /**
     * {@code GET /api/fuel-logs}: all of this user's logs, highest odometer first.
     *
     * @param user the signed-in user
     * @return the logs, with miles driven, MPG and cost filled in
     */
    @GetMapping
    public List<FuelLogResponse> list(AppUser user) {
        return service.findAll(user);
    }

    /**
     * {@code POST /api/fuel-logs}: logs a fill-up.
     *
     * @param user    the signed-in user
     * @param request the fill-up; {@code previousOdometer} is only used for the first log
     * @return {@code 201 Created} with the new log and a {@code Location} header; 400 if a rule is broken
     */
    @PostMapping
    public ResponseEntity<FuelLogResponse> create(AppUser user, @Valid @RequestBody FuelLogRequest request) {
        FuelLogResponse created = service.create(user, request);
        return ResponseEntity
                .created(URI.create("/api/fuel-logs/" + created.id()))
                .body(created);
    }

    /**
     * {@code PUT /api/fuel-logs/{id}}: edits a log; if the odometer changes, the next log starts at the new reading.
     *
     * @param user    the signed-in user
     * @param id      the log to edit
     * @param request the new values
     * @return the edited log; 404 if it isn't this user's, 400 if a rule is broken
     */
    @PutMapping("/{id}")
    public FuelLogResponse update(AppUser user, @PathVariable Long id, @Valid @RequestBody FuelLogRequest request) {
        return service.update(user, id, request);
    }

    /**
     * {@code DELETE /api/fuel-logs/{id}}: deletes a log.
     *
     * @param user the signed-in user
     * @param id   the log to delete
     * @return {@code 204 No Content}; 404 if it isn't this user's
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(AppUser user, @PathVariable Long id) {
        service.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
