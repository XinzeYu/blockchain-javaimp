package com.yxz.transaction;


import com.yxz.block.Block;
import com.yxz.block.Blockchain;
import com.yxz.util.LevelDBUtil;
import com.yxz.util.SerializeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * UTXO池，为了优化查询速度
 * 存储了所有 UTXOs（未花费交易输出）
 * 无需每次查询都去遍历区块链
 * 所缓存的数据需要从构建区块链中所有的交易数据中获得（只需要执行一次即可）
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UTXOSet {

    private Blockchain blockchain;


    /**
     * 重置UTXO池
     */
    @Synchronized
    public void reset() throws Exception {
        System.out.println("Start to reset UTXO set !");
        LevelDBUtil.getInstance().cleanChainStateBucket();
        Map<String, TXOutput[]> allUTXOs = blockchain.findAllUTXOs();
        for (Map.Entry<String, TXOutput[]> entry : allUTXOs.entrySet()) {
            LevelDBUtil.getInstance().putUTXOs(entry.getKey(), entry.getValue());
        }
        System.out.println("Reset UTXO set finished ! ");
    }

    /**
     * 寻找地址对应的能够花费的交易
     *
     * @param publicKeyHash
     * @param amount
     * @return
     * @throws Exception
     */
    public SpendableTXOutput findSpendableTXOutputs(byte[] publicKeyHash, int amount) throws Exception {

        int total = 0;
        Map<String, int[]> unspentTXOs = new HashMap<>();
        Map<String, byte[]> chainstateBucket = LevelDBUtil.getInstance().getChainstateBucket();
        for (Map.Entry<String, byte[]> entry : chainstateBucket.entrySet()) {
            String txId = entry.getKey();
            TXOutput[] outputs = (TXOutput[]) SerializeUtil.deserialize(entry.getValue());

            //遍历所有交易输出
            for (int i = 0; i < outputs.length; i++) {
                TXOutput txOutput = outputs[i];
                //寻找能被地址解锁的交易输出，并且综合小于金额
                if (txOutput.canBeUnlockedWithKey(publicKeyHash) && total < amount) {
                    total += txOutput.getValue();

                    int[] outIds = unspentTXOs.get(txId);
                    if (outIds == null) {
                        outIds = new int[]{i};
                    } else {
                        outIds = ArrayUtils.add(outIds, i);
                    }
                    unspentTXOs.put(txId, outIds);
                    if (total >= amount) {
                        break;
                    }
                }

            }
        }
        return new SpendableTXOutput(total, unspentTXOs);
    }

    /**
     * 查找钱包地址对应的所有UTXO，未花费 意味着这些交易输出从未被交易输入所指向
     *
     * @param
     * @return
     */
    public TXOutput[] findUTXO(byte[] publicKeyHash) throws Exception {
        Map<String, byte[]> chainstateBucket = LevelDBUtil.getInstance().getChainstateBucket();
        TXOutput[] utxos = {};
        if (chainstateBucket.isEmpty()) {
            return utxos;
        }
        for (byte[] value : chainstateBucket.values()) {
            TXOutput[] txOutputs = (TXOutput[]) SerializeUtil.deserialize(value);
            for (TXOutput txOutput : txOutputs) {
                if (txOutput.canBeUnlockedWithKey(publicKeyHash)) {
                    utxos = ArrayUtils.add(utxos, txOutput);
                }
            }
        }
        return utxos;
    }


    /**
     * 更新UTXO池
     * <p>
     * 当一个新的区块产生时，需要去做两件事情：
     * 1）从UTXO池中移除花费掉了的交易输出；
     * 2）保存新的未花费交易输出；
     *
     * @param lastBlock 最新的区块
     */
    @Synchronized
    public void update(Block lastBlock) {
        if (lastBlock == null) {
            System.out.println("Fail to update UTXO set ! lastBlock is null !");
            throw new RuntimeException("Fail to update UTXO set ! ");
        }
        for (Transaction transaction : lastBlock.getTransactions()) {

            // 根据交易输入排查出剩余未被使用的交易输出
            if (!transaction.isCoinbase()) {
                for (TXInput txInput : transaction.getInputs()) {
                    // 余下未被使用的交易输出
                    TXOutput[] remainderUTXOs = {};
                    String txId = Hex.encodeHexString(txInput.getTxId());
                    TXOutput[] txOutputs = LevelDBUtil.getInstance().getUTXOs(txId);

                    if (txOutputs == null) {
                        continue;
                    }

                    for (int outIndex = 0; outIndex < txOutputs.length; outIndex++) {
                        if (outIndex != txInput.getTxOutputIndex()) {
                            remainderUTXOs = ArrayUtils.add(remainderUTXOs, txOutputs[outIndex]);
                        }
                    }

                    //没有剩余则删除，否则更新
                    if (remainderUTXOs.length == 0) {
                        LevelDBUtil.getInstance().deleteUTXOs(txId);
                    } else {
                        LevelDBUtil.getInstance().putUTXOs(txId, remainderUTXOs);
                    }
                }
            }

            //新的交易输出保存到DB中
            TXOutput[] txOutputs = transaction.getOutputs();
            String txId = Hex.encodeHexString(transaction.getTxId());
            LevelDBUtil.getInstance().putUTXOs(txId, txOutputs);
        }

    }

}
