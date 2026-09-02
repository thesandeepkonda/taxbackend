package com.crm.matrix.dto;

import com.crm.matrix.enums.LeaveType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateLeaveRequest {

    // =========================================================
    // LEAVE TYPE
    // =========================================================

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;


    // =========================================================
    // FROM DATE
    // =========================================================

    @NotNull(message = "From date is required")
    @FutureOrPresent(
            message = "From date cannot be in the past"
    )
    private LocalDate fromDate;


    // =========================================================
    // TO DATE
    // =========================================================

    @NotNull(message = "To date is required")
    @FutureOrPresent(
            message = "To date cannot be in the past"
    )
    private LocalDate toDate;


    // =========================================================
    // REASON
    // =========================================================

    @Size(
            max = 1000,
            message = "Description cannot exceed 1000 characters"
    )
    private String description;
}