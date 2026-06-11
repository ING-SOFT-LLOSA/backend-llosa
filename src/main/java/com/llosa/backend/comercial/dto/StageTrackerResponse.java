package com.llosa.backend.comercial.dto;

import java.util.List;

public record StageTrackerResponse(
        StageInfo stage,
        List<StepperItem> stepper,
        StageDetails stageDetails
) {
    public record StageInfo(
            String id,
            String title,
            int stepIndex,
            int totalSteps,
            double progressPercentage
    ) {}

    public record StepperItem(
            String nombreHito,
            String estado,
            int ordenIndex
    ) {}

    public record StageDetails(
            String area,
            String totalPrice,
            String deliveryDate,
            String disbursementDate
    ) {}
}
