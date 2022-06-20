package com.yxz.transaction;

import com.yxz.util.SerializeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;


/**
 * 关于比特币的UTXO模型，实际上是交易输入与交易输出的结合：
 * 1. 有些交易输出并不是由交易输入产生，而是凭空产生的。
 * 2. 但交易输入必须指向某个交易输出，它不能凭空产生。
 * 3. 在一笔交易里面，交易输入可能会来自多笔交易所产生的交易输出。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    /**
     * 挖矿奖励数量
     */
    private static final int bonus = 10;

    /**
     * 交易的Hash值
     */
    private byte[] txId;

    /**
     * 交易输入
     */
    private TXInput[] inputs;

    /**
     * 交易输出
     */
    private TXOutput[] outputs;


    /**
     * 计算交易信息的Hash值
     *
     * @return
     */
    public byte[] hash() {
        byte[] serializeBytes = SerializeUtil.serialize(this);
        Transaction copyTx = (Transaction) SerializeUtil.deserialize(serializeBytes);
        copyTx.setTxId(new byte[]{});
        return DigestUtils.sha256(SerializeUtil.serialize(copyTx));
    }

    /**
     * 创建CoinBase交易，初始交易模块
     *
     * @param to   收账的钱包地址
     * @param data 解锁脚本数据
     * @return
     */
    public static Transaction newCoinbaseTX(String to, String data) {
        if (StringUtils.isBlank(data)) {
            data = String.format("Reward to '%s'", to);
        }
        // 创建交易输入
        TXInput txInput = new TXInput(new byte[]{}, -1, data);

        // 创建交易输出，提供一笔激励
        TXOutput txOutput = new TXOutput(bonus, to);

        // 创建交易
        Transaction tx = new Transaction(null, new TXInput[]{txInput}, new TXOutput[]{txOutput});

        // 设置交易ID
        tx.setTxId(tx.hash());
        return tx;
    }

    /**
     * 判断是否为coinbase交易，交易输入对应一个TXInput txInput = new TXInput(new byte[]{}, -1, data);
     *
     * @return
     */
    public boolean isCoinbase() {
        return this.getInputs().length == 1
                && this.getInputs()[0].getTxId().length == 0
                && this.getInputs()[0].getTxOutputIndex() == -1;
    }
}
