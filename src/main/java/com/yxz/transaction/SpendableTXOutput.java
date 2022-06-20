package com.yxz.transaction;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpendableTXOutput {

    private int total;

    /**
     * 未花费的交易
     */
    private Map<String, int[]> unspentTXOs;
}
