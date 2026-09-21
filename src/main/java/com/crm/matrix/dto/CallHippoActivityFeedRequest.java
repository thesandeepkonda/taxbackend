package com.crm.matrix.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallHippoActivityFeedRequest {

    private String skip;

    private String limit;

    private String startDate;

    private String endDate;

    private String crmUniqueId;

    private String callSid;
}