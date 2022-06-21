package com.yxz.util;


import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;

import java.util.Arrays;

/**
 * 地址工具类
 * 1. 公钥经过算法处理后得到地址
 * 2. 先经过SHA-256算法处理得到32字节的哈希结果
 * 3. 再经过RIPEMED算法处理后得到20字节的摘要结果
 *
 */
public class AddressUtil {
    /**
     * 双重Hash计算
     *
     * @param message
     * @return
     */
    public static byte[] doubleHash(byte[] message) {
        return DigestUtils.sha256(DigestUtils.sha256(message));
    }

    /**
     * 计算公钥的 RIPEMD160 Hash值
     *
     * @param publicKey 公钥
     * @return 经过RIPEMED算法处理后得到20字节的摘要结果
     */
    public static byte[] ripeMD160Hash(byte[] publicKey) {
        byte[] shaHashedKey = DigestUtils.sha256(publicKey);
        RIPEMD160Digest ripemd160 = new RIPEMD160Digest();
        ripemd160.update(shaHashedKey, 0, shaHashedKey.length);
        byte[] output = new byte[ripemd160.getDigestSize()];
        ripemd160.doFinal(output, 0);
        return output;
    }

    /**
     * 生成公钥的校验码，对应步骤：
     * 1. 有系统版本号的数值进行两次次SHA-256计算;
     * 2. 取出最新哈希公钥数值的前4个字节;
     *
     * @param payload
     * @return
     */
    public static byte[] checksum(byte[] payload) {
        return Arrays.copyOfRange(doubleHash(payload), 0, 4);
    }
}
