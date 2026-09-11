package com.crm.matrix.service;

import com.crm.matrix.dto.CalendarEventResponse;
import com.crm.matrix.dto.CreateEventRequest;
import com.crm.matrix.dto.UpdateEventRequest;
import com.crm.matrix.entity.CalendarEvent;
import com.crm.matrix.entity.User;
import com.crm.matrix.enums.EventTargetType;
import com.crm.matrix.repository.CalendarEventRepository;
import com.crm.matrix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final CalendarEventRepository eventRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService; // Optional: to push real-time notifications via SSE

    private User getLoggedInUser(Authentication authentication) {
        return userRepository.findByEmployeeCode(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }


    @Transactional
    public CalendarEventResponse createEvent(CreateEventRequest request, Authentication authentication) {
        User admin = getLoggedInUser(authentication);

        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }

        if (request.getTargetType() != EventTargetType.ALL && request.getTargetId() == null) {
            throw new IllegalArgumentException("Target ID is required for " + request.getTargetType());
        }

        CalendarEvent event = new CalendarEvent();
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setTargetType(request.getTargetType());
        event.setTargetId(request.getTargetId());
        event.setMeetingLink(request.getMeetingLink() != null ? request.getMeetingLink().trim() : null); // Added here
        event.setCreatedBy(admin);

        CalendarEvent saved = eventRepository.save(event);
        broadcastEventNotification(saved);

        return mapToResponse(saved);
    }

    private CalendarEventResponse mapToResponse(CalendarEvent event) {
        String adminName = event.getCreatedBy().getFirstName() +
                (event.getCreatedBy().getLastName() != null ? " " + event.getCreatedBy().getLastName() : "");

        return CalendarEventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .targetType(event.getTargetType())
                .targetId(event.getTargetId())
                .createdByName(adminName)
                .meetingLink(event.getMeetingLink()) // Added here
                .build();
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getMyCalendarEvents(LocalDateTime fromDate, LocalDateTime toDate, Authentication authentication) {
        User user = getLoggedInUser(authentication);
        Long teamId = user.getTeam() != null ? user.getTeam().getId() : -1L;
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : -1L;

        List<CalendarEvent> events = eventRepository.findEventsForUserInRange(
                user.getId(), teamId, departmentId, fromDate, toDate
        );

        return events.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getAllEvents() {
        List<CalendarEvent> events = eventRepository.findAllByOrderByStartTimeAsc();
        return events.stream()
                .map(this::mapToResponse)
                .toList();
    }
    private void broadcastEventNotification(CalendarEvent event) {
        String title = "New Event: " + event.getTitle();
        String message = "Scheduled for: " + event.getStartTime().toString();

        if (event.getMeetingLink() != null && !event.getMeetingLink().isBlank()) {
            message += " | Link: " + event.getMeetingLink();
        }

        List<User> recipients = new java.util.ArrayList<>();

        switch (event.getTargetType()) {
            case INDIVIDUAL -> userRepository.findById(event.getTargetId()).ifPresent(recipients::add);
            case TEAM -> recipients.addAll(userRepository.findByTeamIdAndActiveTrue(event.getTargetId()));
            case DEPARTMENT -> recipients.addAll(userRepository.findByDepartmentIdAndActiveTrue(event.getTargetId()));
            case ALL -> recipients.addAll(userRepository.findByActiveTrue());
        }

        for (User user : recipients) {
            notificationService.sendNotification(user, title, message, "CALENDAR_EVENT","/calendar");
        }
    }

    @Transactional
    public CalendarEventResponse updateEvent(Long id, UpdateEventRequest request, Authentication authentication) {
        User admin = getLoggedInUser(authentication);

        CalendarEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + id));

        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }

        if (request.getTargetType() != EventTargetType.ALL && request.getTargetId() == null) {
            throw new IllegalArgumentException("Target ID is required for " + request.getTargetType());
        }

        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setTargetType(request.getTargetType());
        event.setTargetId(request.getTargetId());
        event.setMeetingLink(request.getMeetingLink() != null ? request.getMeetingLink().trim() : null);

        CalendarEvent updated = eventRepository.save(event);

        // Optional: Broadcast notification or return response
        return mapToResponse(updated);
    }

    // =========================================================
    // DELETE EVENT
    // =========================================================
    @Transactional
    public void deleteEvent(Long id, Authentication authentication) {
        // Ensure user/admin exists and is authenticated
        getLoggedInUser(authentication);

        CalendarEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + id));

        eventRepository.delete(event);
    }

}