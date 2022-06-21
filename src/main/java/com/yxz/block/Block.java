package com.yxz.block;

import com.yxz.consensus.PowResult;
import com.yxz.consensus.ProofOfWork;
import com.yxz.transaction.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.time.Instant;


@Data
@NoArgsConstructor
@AllArgsConstructor
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
     * 区块存储交易信息
     */
    private Transaction[] transactions;

    /**
     * 区块时间戳
     */
    private long timeStamp;

    /**
     * 随机数，计算PoW的随机数值
     */
    private long nonce;


    /**
     *
     * @param preHash
     * @param transactions
     * @return
     */
    public static Block createNewBlock(String preHash, Transaction[] transactions) {
        Block block = new Block("", preHash, transactions, Instant.now().getEpochSecond(),0);
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


    public static Block newGenesisBlock(Transaction coinbase) {
        return Block.createNewBlock("", new Transaction[]{coinbase});
    }

    /**
     * 对区块中的交易信息进行Hash计算，理论上需要用Merkel树表示
     *
     * @return
     */
    /*public byte[] hashTransaction() {
        byte[][] txIdArrays = new byte[this.getTransactions().length][];
        for (int i = 0; i < this.getTransactions().length; i++) {
            txIdArrays[i] = this.getTransactions()[i].getTxId();
        }
        return DigestUtils.sha256(ByteUtils.merge(txIds));
    }*/
}
