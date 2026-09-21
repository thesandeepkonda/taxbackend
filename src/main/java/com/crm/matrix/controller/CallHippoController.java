package com.crm.matrix.controller;

import com.crm.matrix.config.PhoneNumberUtils;
import com.crm.matrix.dto.CallHippoActivityFeedRequest;
import com.crm.matrix.dto.CallHippoReminderRequest;
import com.crm.matrix.dto.CallHippoReminderResponse;
import com.crm.matrix.dto.CallHistoryResponse;
import com.crm.matrix.entity.CallHistory;
import com.crm.matrix.entity.Client;
import com.crm.matrix.entity.User;
import com.crm.matrix.repository.CallHistoryRepository;
import com.crm.matrix.repository.ClientRepository;
import com.crm.matrix.repository.UserRepository;
import com.crm.matrix.service.CallHippoAdminService;
import com.crm.matrix.service.CallHippoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/callhippo")
@RequiredArgsConstructor
public class CallHippoController {

    private final CallHippoService callHippoService;

    private final ClientRepository clientRepository;

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private final CallHistoryRepository callHistoryRepository;
    private final CallHippoAdminService callHippoAdminService;



    @PostMapping("/call/{clientId}")

    public ResponseEntity<?> callClient(
            @PathVariable Long clientId,
            Authentication authentication
    ) {

        try {


            String employeeCode =
                    authentication.getName();

            System.out.println(
                    "Logged-in employeeCode: "
                            + employeeCode
            );


            User user =
                    userRepository
                            .findByEmployeeCode(
                                    employeeCode
                            )
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Logged-in employee not found: "
                                                    + employeeCode
                                    )
                            );


            Client client =
                    clientRepository
                            .findById(clientId)
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Client not found: "
                                                    + clientId
                                    )
                            );

            CallHistory callHistory =
                    callHippoService.makeCall(
                            user,
                            client
                    );

            return ResponseEntity.ok(
                    new CallResponse(
                            true,
                            "Call initiated successfully",
                            callHistory.getId(),
                            callHistory.getCallSid(),
                            callHistory.getStatus()
                    )
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(
                            new CallResponse(
                                    false,
                                    ex.getMessage(),
                                    null,
                                    null,
                                    "FAILED"
                            )
                    );
        }
    }



    @PatchMapping("/employee/available")
    public ResponseEntity<?> setAvailable(
            Authentication authentication
    ) {

        try {

            String employeeCode =
                    authentication.getName();

            User user =
                    userRepository
                            .findByEmployeeCode(
                                    employeeCode
                            )
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Logged-in employee not found: "
                                                    + employeeCode
                                    )
                            );

            boolean success =
                    callHippoService.setUserAvailable(
                            user
                    );

            return ResponseEntity.ok(
                    new StatusResponse(
                            success,
                            "CallHippo employee status changed to Available"
                    )
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(
                            new StatusResponse(
                                    false,
                                    ex.getMessage()
                            )
                    );
        }
    }


    @PatchMapping("/employee/unavailable")
    public ResponseEntity<?> setUnavailable(
            Authentication authentication
    ) {

        try {

            String employeeCode =
                    authentication.getName();

            User user =
                    userRepository
                            .findByEmployeeCode(
                                    employeeCode
                            )
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Logged-in employee not found: "
                                                    + employeeCode
                                    )
                            );

            boolean success =
                    callHippoService.setUserUnavailable(
                            user
                    );

            return ResponseEntity.ok(
                    new StatusResponse(
                            success,
                            "CallHippo employee status changed to UnAvailable"
                    )
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(
                            new StatusResponse(
                                    false,
                                    ex.getMessage()
                            )
                    );
        }
    }



    @GetMapping("/client/{clientId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TEAM_LEAD')")
    public ResponseEntity<Page<CallHistoryResponse>>
    getClientCallHistory(

            @PathVariable Long clientId,

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "20"
            )
            int size
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        Page<CallHistory> callHistoryPage =
                callHistoryRepository
                        .findByClientId(
                                clientId,
                                pageable
                        );

        Page<CallHistoryResponse> responsePage =
                callHistoryPage.map(
                        CallHistoryResponse::from
                );

        return ResponseEntity.ok(
                responsePage
        );
    }



    public record CallResponse(

            boolean success,

            String message,

            Long callHistoryId,

            String callSid,

            String status

    ) {
    }


    public record StatusResponse(

            boolean success,

            String message

    ) {
    }

    @GetMapping("/employee/calls")
    public ResponseEntity<?> getMyCalls(

            Authentication authentication,

            @RequestParam(required = false)
            Long clientId,

            @RequestParam(required = false)
            String startDate,

            @RequestParam(required = false)
            String endDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        try {

            String employeeCode =
                    authentication.getName();

            User user =
                    userRepository
                            .findByEmployeeCode(
                                    employeeCode
                            )
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Logged-in employee not found: "
                                                    + employeeCode
                                    )
                            );



            LocalDate from;

            LocalDate to;

            if (startDate != null
                    && !startDate.isBlank()) {

                from =
                        LocalDate.parse(
                                startDate
                        );

            } else {

                from =
                        LocalDate.now()
                                .withDayOfMonth(1);
            }

            if (endDate != null
                    && !endDate.isBlank()) {

                to =
                        LocalDate.parse(
                                endDate
                        );

            } else {

                to =
                        LocalDate.now();
            }


            int skip =
                    page * size;

            callHippoService.syncActivityFeed(
                    user,
                    from,
                    to,
                    skip,
                    size
            );

            /*
             * =====================================================
             * GET CRM CALL HISTORY
             * =====================================================
             */

            Page<CallHistoryResponse> response =
                    callHippoAdminService.getEmployeeCalls(
                            employeeCode,
                            clientId,
                            from,
                            to,
                            page,
                            size
                    );

            return ResponseEntity.ok(
                    response
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(
                            new StatusResponse(
                                    false,
                                    ex.getMessage()
                            )
                    );
        }
    }

    @GetMapping("/embedded/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TEAM_LEAD')")
    public ResponseEntity<?> getEmbeddedConfig(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                callHippoService.getEmbeddedConfig(authentication)
        );
    }

    @PostMapping("/reminder")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TEAM_LEAD')")
    public ResponseEntity<?> createReminder(
            @RequestBody CallHippoReminderRequest request,
            Authentication authentication
    ) {

        try {

            String employeeCode =
                    authentication.getName();

            System.out.println(
                    "Creating CallHippo reminder for employee: "
                            + employeeCode
            );

            User user =
                    userRepository
                            .findByEmployeeCode(employeeCode)
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Logged-in employee not found: "
                                                    + employeeCode
                                    )
                            );

            Client client =
                    clientRepository
                            .findById(request.getClientId())
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Client not found: "
                                                    + request.getClientId()
                                    )
                            );

            Object callHippoResponse =
                    callHippoService.createReminder(
                            user,
                            client,
                            request.getReminderTime()
                    );

            return ResponseEntity.ok(
                    new CallHippoReminderResponse(
                            true,
                            "Call reminder created successfully",
                            callHippoResponse
                    )
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(
                            new CallHippoReminderResponse(
                                    false,
                                    ex.getMessage(),
                                    null
                            )
                    );
        }
    }

//    @GetMapping("/recordings/history/{callHistoryId}")
//    public ResponseEntity<byte[]> getRecording(
//            @PathVariable Long callHistoryId,
//            Authentication authentication
//    ) {
//
//        String employeeCode = authentication.getName();
//
//        User user =
//                userRepository
//                        .findByEmployeeCode(employeeCode)
//                        .orElseThrow(() ->
//                                new RuntimeException(
//                                        "Logged-in employee not found: "
//                                                + employeeCode
//                                )
//                        );
//
//        return callHippoService.getRecording(
//                user,
//                callHistoryId
//        );
//    }

    @PostMapping("/activityfeed")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TEAM_LEAD')")
    public ResponseEntity<?> activityFeed(
            @RequestBody CallHippoActivityFeedRequest request,
            Authentication authentication
    ) {

        try {

            String employeeCode =
                    authentication.getName();

            User user =
                    userRepository
                            .findByEmployeeCode(employeeCode)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Logged-in employee not found: "
                                                    + employeeCode
                                    )
                            );

            String response =
                    callHippoService.getActivityFeed(
                            user,
                            request.getCallSid(),
                            request.getStartDate(),
                            request.getEndDate(),
                            request.getCrmUniqueId(),
                            request.getSkip(),
                            request.getLimit()
                    );

            /*
             * IMPORTANT:
             * Return the JSON string directly.
             *
             * Do NOT use:
             *
             * objectMapper.readTree(response)
             */
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception ex) {

            ex.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "success",
                                    false,
                                    "message",
                                    ex.getMessage()
                            )
                    );
        }
    }

    @GetMapping("/whatsapp/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TEAM_LEAD')")
    public ResponseEntity<?> openWhatsApp(
            @PathVariable Long clientId
    ) {

        Client client =
                clientRepository
                        .findById(clientId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Client not found: " + clientId
                                )
                        );

        String phone = client.getPhone();

        if (phone == null || phone.isBlank()) {
            throw new RuntimeException(
                    "Client phone number is not available"
            );
        }

        String normalizedPhone =
                PhoneNumberUtils.normalize(phone);

        if (normalizedPhone == null
                || normalizedPhone.isBlank()) {

            throw new RuntimeException(
                    "Invalid client phone number"
            );
        }

        String whatsappNumber =
                normalizedPhone.replaceAll(
                        "[^0-9]",
                        ""
                );

        String whatsappUrl =
                "https://wa.me/" + whatsappNumber;

        return ResponseEntity.ok(
                new WhatsAppResponse(
                        true,
                        whatsappUrl
                )
        );
    }
    public record WhatsAppResponse(
            boolean success,
            String url
    ) {
    }
}