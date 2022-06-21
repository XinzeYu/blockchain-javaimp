package com.yxz.util;


import java.math.BigInteger;

/**
 * Base58工具类
 * 比特币的 Base58 字母表：
 * 123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz
 * Base58Check 是一种常用在比特币中的 Base58 编码格式，增加了错误校验码来检查数据在转录中出现的错误。
 * 校验码长 4 个字节，添加到需要编码的数据之后。
 *
 *
 */
public class Base58Util {


    //Base58字母表
    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";


    private static final BigInteger ALPHABET_SIZE = BigInteger.valueOf(ALPHABET.length());


    /**
     * 转化为 Base58 字符串
     *
     * @param data
     * @return
     */
    public static String rawBytesToBase58(byte[] data) {
        // Convert to base-58 string
        StringBuilder sb = new StringBuilder();
        BigInteger num = new BigInteger(1, data);
        while (num.signum() != 0) {
            BigInteger[] quotrem = num.divideAndRemainder(ALPHABET_SIZE);
            sb.append(ALPHABET.charAt(quotrem[1].intValue()));
            num = quotrem[0];
        }

        // Add '1' characters for leading 0-value bytes
        for (int i = 0; i < data.length && data[i] == 0; i++) {
            sb.append(ALPHABET.charAt(0));
        }
        return sb.reverse().toString();
    }
}
