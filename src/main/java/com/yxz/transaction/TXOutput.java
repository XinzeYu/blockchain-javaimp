package com.yxz.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 *
 * 交易输出是”Coins” 实际存储的地方，交易输出先于交易输入出现
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TXOutput {
    /**
     * 数值
     */
    private int value;

    /**
     * 锁定脚本，会存储任意的字符串（用户定义的钱包地址）
     */
    private String scriptPubKey;


    /**
     * 判断解锁数据是否能够解锁交易输出
     *
     * @param unlockingData
     * @return
     */
    public boolean canBeUnlockedWith(String unlockingData) {
        return this.getScriptPubKey().endsWith(unlockingData);
    }
}
