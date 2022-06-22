package com.yxz.transaction;


import com.yxz.util.AddressUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;

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
     * 签名
     */
    private byte[] signature;
    /**
     * 公钥
     */
    private byte[] publicKey;


    /**
     * 用于检查交易输入中的公钥是否能够解锁交易输出
     *
     * @param
     * @return
     */
    public boolean canUnlockOutputWithKey(byte[] publicKeyHash) {
        byte[] lockingHash = AddressUtil.ripeMD160Hash(this.getPublicKey());
        return Arrays.equals(lockingHash, publicKeyHash);
    }
}
