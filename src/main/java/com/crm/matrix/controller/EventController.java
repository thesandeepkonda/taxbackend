package com.crm.matrix.controller;

import com.crm.matrix.dto.CalendarEventResponse;
import com.crm.matrix.dto.CreateEventRequest;
import com.crm.matrix.dto.UpdateEventRequest;
import com.crm.matrix.security.HasPermission;
import com.crm.matrix.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @HasPermission("EVENT_CREATE")
    @PostMapping
    public ResponseEntity<CalendarEventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createEvent(request, authentication));
    }

    @GetMapping("/calendar")
    public ResponseEntity<List<CalendarEventResponse>> getCalendarEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            Authentication authentication) {
        return ResponseEntity.ok(eventService.getMyCalendarEvents(fromDate, toDate, authentication));
    }
    @HasPermission("EVENT_CREATE")
    @GetMapping("/all")
    public ResponseEntity<List<CalendarEventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }
    @PutMapping("/{id}")
    @HasPermission("EVENT_CREATE")
    public ResponseEntity<CalendarEventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(eventService.updateEvent(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    @HasPermission("EVENT_CREATE")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            Authentication authentication) {
        eventService.deleteEvent(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
