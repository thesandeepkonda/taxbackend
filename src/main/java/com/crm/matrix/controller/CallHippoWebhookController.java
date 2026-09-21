package com.crm.matrix.controller;

import com.crm.matrix.dto.CallHippoWebhookRequest;
import com.crm.matrix.service.CallHippoWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/callhippo")
@RequiredArgsConstructor
public class CallHippoWebhookController {

    private final CallHippoWebhookService callHippoWebhookService;

    @PostMapping("/webhook")
    public ResponseEntity<?> callHippoWebhook(
            @RequestBody CallHippoWebhookRequest request
    ) {

        try {

            System.out.println("======================================");
            System.out.println("CALLHIPPO WEBHOOK RECEIVED");
            System.out.println("Activity Type : " + request.getActivityType());
            System.out.println("Call SID      : " + request.getCallSid());
            System.out.println("From Number   : " + request.getFromNumber());
            System.out.println("To Number     : " + request.getToNumber());
            System.out.println("Call Type     : " + request.getCallType());
            System.out.println("Status        : " + request.getStatus());
            System.out.println("Duration      : " + request.getDuration());
            System.out.println("Email         : " + request.getEmail());
            System.out.println("======================================");

            callHippoWebhookService.process(request);

            return ResponseEntity.ok(
                    new CallHippoController.StatusResponse(
                            true,
                            "CallHippo webhook processed successfully"
                    )
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(
                            new CallHippoController.StatusResponse(
                                    false,
                                    ex.getMessage()
                            )
                    );
        }
    }
}