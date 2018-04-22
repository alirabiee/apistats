package com.alirabiee.apistats.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Transaction implements Comparable<Transaction> {
    private Double amount;
    private Long timestamp;

    @Override
    public int compareTo(Transaction o) {
        return amount.compareTo(o.amount);
    }
}
