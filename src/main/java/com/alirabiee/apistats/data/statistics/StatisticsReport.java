package com.alirabiee.apistats.data.statistics;

import lombok.*;

/**
 * Contains the reported transaction statistics over a period of time
 */
@Value
@AllArgsConstructor
public class StatisticsReport {
    private double sum;
    private double avg;
    private double max;
    private double min;
    private int count;
}
