package com.crm.matrix.controller;


import com.crm.matrix.dto.CallResponseDto;
import com.crm.matrix.dto.StartCallRequestDto;
import com.crm.matrix.enums.CallStatus;
import com.crm.matrix.service.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee/clients")
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;



    @PostMapping("/{clientId}/call/start")
    public ResponseEntity<CallResponseDto> startCall(
            @PathVariable Long clientId,
            @RequestBody(required = false)
            StartCallRequestDto request,
            Authentication authentication) {

        if (request == null) {
            request = new StartCallRequestDto();
        }

        return ResponseEntity.ok(
                callService.startCall(
                        clientId,
                        request,
                        authentication
                )
        );
    }




    @PostMapping("/call/{callId}/end")
    public ResponseEntity<CallResponseDto> endCall(
            @PathVariable Long callId,
            @RequestParam CallStatus status,
            @RequestParam(required = false)
            String recordingUrl,
            Authentication authentication) {

        return ResponseEntity.ok(
                callService.endCall(
                        callId,
                        status,
                        recordingUrl,
                        authentication
                )
        );
    }
}