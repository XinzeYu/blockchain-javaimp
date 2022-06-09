package com.yxz.block;

import com.yxz.consensus.PowResult;
import com.yxz.consensus.ProofOfWork;
import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.time.Instant;


@Data
public class Block {
    /**
     * 区块的哈希值
     */
    private String hash;

    /**
     * 前一区块的哈希值
     */
    private String preHash;

    /**
     * 区块存储数据
     */
    private String data;

    /**
     * 区块时间戳
     */
    private long timeStamp;

    /**
     * 随机数，计算PoW的随机数值
     */
    private long nonce;

    public Block() {

    }

    public Block(String hash, String preHash, String data, long timeStamp) {
        this.hash = hash;
        this.preHash = preHash;
        this.data = data;
        this.timeStamp = timeStamp;
    }

    /**
     *
     * @param preHash
     * @param data
     * @return
     */
    public static Block createNewBlock(String preHash, String data) {
        Block block = new Block("", preHash, data, Instant.now().getEpochSecond());
        //block.setHash();
        ProofOfWork pow = ProofOfWork.newProofOfWork(block);
        PowResult powResult = pow.run();
        block.setHash(powResult.getHash());
        block.setNonce(powResult.getNonce());
        return block;
    }

    /**
     * 测试函数
     * 计算当前块的哈希值
     * @return
     */
    /*public void setHash() {
        String calculatedhash = StringUtil.applySha256(
                this.preHash +
                        Long.toString(this.timeStamp) +
                        this.data
        );
        this.setHash(calculatedhash);
    }*/


    public static Block newGenesisBlock() {
        return Block.createNewBlock("", "Genesis Block");
    }
}
