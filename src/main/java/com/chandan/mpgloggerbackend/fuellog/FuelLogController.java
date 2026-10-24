package com.chandan.mpgloggerbackend.fuellog;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fuel-logs")
public class FuelLogController {

    private final FuelLogService service;

    public FuelLogController(FuelLogService service) {
        this.service = service;
    }

    /** All logs, highest odometer first. */
    @GetMapping
    public List<FuelLogResponse> list() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<FuelLogResponse> create(@Valid @RequestBody FuelLogRequest request) {
        FuelLogResponse created = service.create(request);
        return ResponseEntity
                .created(URI.create("/api/fuel-logs/" + created.id()))
                .body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
