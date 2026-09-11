package com.crm.matrix.controller;

import com.crm.matrix.dto.CallHistoryResponse;
import com.crm.matrix.entity.CallHistory;
import com.crm.matrix.entity.Client;
import com.crm.matrix.entity.User;
import com.crm.matrix.repository.CallHistoryRepository;
import com.crm.matrix.repository.ClientRepository;
import com.crm.matrix.repository.UserRepository;
import com.crm.matrix.service.CallHippoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/callhippo")
@RequiredArgsConstructor
public class CallHippoController {

    private final CallHippoService callHippoService;

    private final ClientRepository clientRepository;

    private final UserRepository userRepository;

    private final CallHistoryRepository callHistoryRepository;



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

            /*
             * Find client.
             */
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


    /**
     * ============================================================
     * SET CURRENT EMPLOYEE UNAVAILABLE
     * ============================================================
     *
     * PATCH:
     *
     * /api/callhippo/employee/unavailable
     */
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


    /**
     * ============================================================
     * GET CLIENT CALL HISTORY
     * ============================================================
     *
     * GET:
     *
     * /api/callhippo/client/{clientId}/history
     */
    @GetMapping("/client/{clientId}/history")
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


    /**
     * ============================================================
     * RESPONSE DTOs
     * ============================================================
     */

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
}