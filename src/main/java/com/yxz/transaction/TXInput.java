package com.yxz.transaction;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一笔交易的交易输入其实是指向上一笔交易的交易输出
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TXInput {

    /**
     * 交易的hash值
     */
    private byte[] txId;

    /**
     * 交易输出索引
     */
    private int txOutputIndex;

    /**
     * 解锁脚本，提供用于交易输出中 ScriptPubKey 所需的验证数据。
     */
    private String scriptSig;


    /**
     * 判断解锁数据是否能够解锁交易输出
     *
     * @param unlockingData
     * @return
     */
    public boolean canUnlockOutputWith(String unlockingData) {
        return this.getScriptSig().endsWith(unlockingData);
    }
}
