package com.yxz.consensus;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PowResult {
    /**
     * 计数器
     */
    private long nonce;
    /**
     * hash值
     */
    private String hash;

}
