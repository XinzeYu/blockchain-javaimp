package com.yxz.transaction;

import com.yxz.util.Base58Util;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;


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
     * 公钥Hash
     */
    private byte[] publicKeyHash;


    /**
     * 创建交易输出
     *
     * @param value
     * @param address
     * @return
     */
    public static TXOutput newTXOutput(int value, String address) {
        // 反向转化为 byte 数组
        byte[] versionedPayload = Base58Util.base58ToBytes(address);
        byte[] pubKeyHash = Arrays.copyOfRange(versionedPayload, 1, versionedPayload.length);
        return new TXOutput(value, pubKeyHash);
    }


    /**
     * 检查提供的公钥 Hash 是否能够用于解锁交易输出
     *
     * @param
     * @return
     */
    public boolean canBeUnlockedWithKey(byte[] publicKeyHash) {
        return Arrays.equals(this.getPublicKeyHash(), publicKeyHash);
    }
}
