package com.yxz.block;

import com.yxz.consensus.ProofOfWork;
import com.yxz.transaction.SpendableTXOutput;
import com.yxz.transaction.TXInput;
import com.yxz.transaction.TXOutput;
import com.yxz.transaction.Transaction;
import com.yxz.util.LevelDBUtil;
import lombok.Data;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class Blockchain {

    /**
     * 区块链本质是一个块与块相连的链式结构，实现持久化存储后，只记录最新一个区块链的Hash值，随后根据该hash值可以迭代整个区块链
     */
    private String lastBlockHash;

    public Blockchain(String lastBlockHash) {
        this.lastBlockHash = lastBlockHash;;
    }

    public void mineBlock(Transaction[] transactions) throws Exception {
        String lastBlockHash = LevelDBUtil.getInstance().getLastBlockHash();
        if (StringUtils.isBlank(lastBlockHash)) {
            throw new Exception("Fail to add block into blockchain ! ");
        }
        this.addBlock(Block.createNewBlock(lastBlockHash, transactions));
    }

    public void addBlock(Block block) {
        LevelDBUtil.getInstance().putLastBlockHash(block.getHash());
        LevelDBUtil.getInstance().putBlock(block);
        this.lastBlockHash = block.getHash();
    }


    public static Blockchain newBlockchain(String address) {
        String lastBlockHash = LevelDBUtil.getInstance().getLastBlockHash();
        if (StringUtils.isBlank(lastBlockHash)) {
            // 创建 coinBase 交易，赋予创世区块
            Transaction coinbaseTX = Transaction.newCoinbaseTX(address, "");
            Block genesisBlock = Block.newGenesisBlock(coinbaseTX);
            lastBlockHash = genesisBlock.getHash();
            LevelDBUtil.getInstance().putBlock(genesisBlock);
            LevelDBUtil.getInstance().putLastBlockHash(lastBlockHash);
        }
        return new Blockchain(lastBlockHash);
    }

    /**
     * 区块链迭代器
     */
    public class BlockchainIterator {

        private String currentBlockHash;

        public BlockchainIterator(String currentBlockHash) {
            this.currentBlockHash = currentBlockHash;
        }

        /**
         * 是否有下一个区块
         *
         * @return
         */
        public boolean hashNext() throws Exception {
            if (StringUtils.isBlank(currentBlockHash)) {
                return false;
            }
            Block lastBlock = LevelDBUtil.getInstance().getBlock(currentBlockHash);
            if (lastBlock == null) {
                return false;
            }
            // 创世区块直接放行
            if (lastBlock.getPreHash().length() == 0) {
                return true;
            }
            return LevelDBUtil.getInstance().getBlock(lastBlock.getPreHash()) != null;
        }


        /**
         *
         * @return Block
         * @throws Exception
         */
        public Block next() throws Exception {
            Block currentBlock = LevelDBUtil.getInstance().getBlock(currentBlockHash);
            if (currentBlock != null) {
                this.currentBlockHash = currentBlock.getPreHash();
                return currentBlock;
            }
            return null;
        }
    }

    public BlockchainIterator getBlockchainIterator() {
        return new BlockchainIterator(lastBlockHash);
    }



    /**
     * 从交易输入中查询区块链中所有已被花费了的交易输出，即被交易输入所指向的所有交易输出
     *
     * @param address 地址
     * @return 交易ID以及对应的交易输出下标地址
     * @throws Exception
     */
    private Map<String, int[]> getAllSpentTXOs(String address) throws Exception {

        Map<String, int[]> spentTXOs = new HashMap<>();
        for (BlockchainIterator blockchainIterator = this.getBlockchainIterator(); blockchainIterator.hashNext(); ) {
            Block block = blockchainIterator.next();

            for (Transaction transaction : block.getTransactions()) {
                // 如果是 coinbase 交易，直接跳过
                if (transaction.isCoinbase()) {
                    continue;
                }
                for (TXInput txInput : transaction.getInputs()) {
                    // 判断能否被该地址解锁
                    if (txInput.canUnlockOutputWith(address)) {
                        //byte[]转为String
                        String inTxId = Hex.encodeHexString(txInput.getTxId());

                        int[] spentOutIndexArray = spentTXOs.get(inTxId);
                        if (spentOutIndexArray == null) {
                            spentTXOs.put(inTxId, new int[]{txInput.getTxOutputIndex()});
                        } else {
                            spentOutIndexArray = ArrayUtils.add(spentOutIndexArray, txInput.getTxOutputIndex());
                            spentTXOs.put(inTxId, spentOutIndexArray);
                        }
                    }
                }
            }
        }
        return spentTXOs;
    }

    /**
     * 查找钱包地址对应的所有未花费的交易，遍历所有交易，排除被花费的交易输出
     *
     * @param address 地址
     * @return
     * @throws Exception
     */
    private Transaction[] findUnspentTransactions(String address) throws Exception {
        // 获取地址对应所有已被花费了的交易输出，即被交易输入所指向的所有交易输出
        Map<String, int[]> allSpentTXOs = this.getAllSpentTXOs(address);
        Transaction[] unspentTxs = {};

        // 再次遍历所有区块中的交易输出
        for (BlockchainIterator blockchainIterator = this.getBlockchainIterator(); blockchainIterator.hashNext(); ) {
            Block block = blockchainIterator.next();
            for (Transaction transaction : block.getTransactions()) {

                String txId = Hex.encodeHexString(transaction.getTxId());

                int[] spentOutIndexArray = allSpentTXOs.get(txId);

                for (int i = 0; i < transaction.getOutputs().length; i++) {
                    if (spentOutIndexArray != null && ArrayUtils.contains(spentOutIndexArray, i)) {
                        continue;
                    }
                    // 保存不存在 allSpentTXOs 中的交易
                    if (transaction.getOutputs()[i].canBeUnlockedWith(address)) {
                        unspentTxs = ArrayUtils.add(unspentTxs, transaction);
                    }
                }
            }
        }
        return unspentTxs;
    }

    /**
     * 查找钱包地址对应的所有UTXO，未花费 意味着这些交易输出从未被交易输入所指向
     *
     * @param address 钱包地址
     * @return
     */
    public TXOutput[] findUTXO(String address) throws Exception {
        Transaction[] unspentTxs = this.findUnspentTransactions(address);
        TXOutput[] utxos = {};
        if (unspentTxs == null || unspentTxs.length == 0) {
            return utxos;
        }
        for (Transaction tx : unspentTxs) {
            for (TXOutput txOutput : tx.getOutputs()) {
                if (txOutput.canBeUnlockedWith(address)) {
                    utxos = ArrayUtils.add(utxos, txOutput);
                }
            }
        }
        return utxos;
    }

    /**
     * 查询钱包余额，需要回顾所有的UTXO并且相加
     *
     * @param address 钱包地址
     */
    private void getBalance(String address) throws Exception {
        Blockchain blockchain = Blockchain.newBlockchain(address);
        TXOutput[] txOutputs = blockchain.findUTXO(address);
        int balance = 0;
        if (txOutputs != null && txOutputs.length > 0) {
            for (TXOutput txOutput : txOutputs) {
                balance += txOutput.getValue();
            }
        }
        System.out.printf("Balance of '%s': %d\n", address, balance);
    }

    /**
     * 寻找地址对应的能够花费的交易
     *
     * @param address
     * @param amount
     * @return
     * @throws Exception
     */
    public SpendableTXOutput findSpendableTXOutputs(String address, int amount) throws Exception {
        //首先找到所有未花费的交易
        Transaction[] unspentTXs = this.findUnspentTransactions(address);
        int total = 0;
        Map<String, int[]> unspentTXOs = new HashMap<>();
        for (Transaction tx : unspentTXs) {
            String txId = Hex.encodeHexString(tx.getTxId());
            //遍历所有交易输出
            for (int i = 0; i < tx.getOutputs().length; i++) {
                TXOutput txOutput = tx.getOutputs()[i];
                //寻找能被地址解锁的交易输出，并且综合小于金额
                if (txOutput.canBeUnlockedWith(address) && total < amount) {
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

    public static void main(String[] args) {
        try {
            Blockchain blockchain = Blockchain.newBlockchain("yxz");
            blockchain.getBalance("yxz");
            System.out.println(LevelDBUtil.getInstance().getLastBlockHash());

            Transaction transaction = Transaction.newTransaction("yxz", "yzq", 5, blockchain);
            blockchain.mineBlock(new Transaction[]{transaction});
            System.out.println("Success!");

            blockchain.getBalance("yzq");
            blockchain.getBalance("yxz");
            LevelDBUtil.getInstance().closeDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
