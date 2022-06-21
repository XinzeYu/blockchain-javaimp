package com.yxz.consensus;

import com.yxz.block.Block;
import com.yxz.util.StringUtil;
import lombok.Data;

import java.math.BigInteger;

@Data
public class ProofOfWork {

    /**
     * 难度目标位，按道理应当动态调整
     */
    public static final int TARGET_BITS = 20;

    /**
     * 区块
     */
    private Block block;

    /**
     * 难度目标值
     */
    private BigInteger target;

    public ProofOfWork(Block block, BigInteger target) {
        this.block = block;
        this.target = target;
    }

    /**
     * 创建新的工作量证明，设定难度目标值
     * <p>
     * 对1进行移位运算，将1向左移动 (256 - TARGET_BITS) 位，得到我们的难度目标值
     *
     * @param block
     * @return
     */
    public static ProofOfWork newProofOfWork(Block block) {
        //得到的目标为10000000......
        BigInteger targetValue = BigInteger.valueOf(1).shiftLeft((256 - TARGET_BITS));
        return new ProofOfWork(block, targetValue);
    }

    /**
     * 运行工作量证明，开始挖矿，找到小于难度目标值的Hash
     *
     * @return
     */
    public PowResult run() {
        long nonce = 0;
        String shaHex = "";
        System.out.printf("Mining the block containing：%s \n", this.getBlock().getTransactions());

        long startTime = System.currentTimeMillis();
        while (nonce < Long.MAX_VALUE) {
            String data = this.prepareData(nonce);
            shaHex = StringUtil.applySha256(data);
            if (new BigInteger(shaHex, 16).compareTo(this.target) == -1) {
                System.out.printf("Elapsed Time: %s seconds \n", (float) (System.currentTimeMillis() - startTime) / 1000);
                System.out.printf("correct hash Hex: %s \n\n", shaHex);
                break;
            } else {
                nonce++;
            }
        }
        return new PowResult(nonce, shaHex);
    }

    /**
     * 验证区块是否有效
     *
     * @return
     */
    public boolean validate() {
        String data = this.prepareData(this.getBlock().getNonce());
        return new BigInteger(StringUtil.applySha256(data), 16).compareTo(this.target) == -1;
    }

    /**
     * 准备数据
     *
     * @param nonce
     * @return
     */
    private String prepareData(long nonce) {
        return this.getBlock().getPreHash() +
                this.getBlock().getTransactions() + this.getBlock().getTimeStamp() + TARGET_BITS + nonce;
    }




}
