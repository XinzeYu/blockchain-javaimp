package com.yxz.block;

import com.yxz.consensus.ProofOfWork;
import com.yxz.transaction.SpendableTXOutput;
import com.yxz.transaction.TXInput;
import com.yxz.transaction.TXOutput;
import com.yxz.transaction.Transaction;
import com.yxz.util.Base58Util;
import com.yxz.util.LevelDBUtil;
import com.yxz.util.WalletUtil;
import com.yxz.wallet.Wallet;
import lombok.Data;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

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
     * @param
     * @return 交易ID以及对应的交易输出下标地址
     * @throws Exception
     */
    private Map<String, int[]> getAllSpentTXOs(byte[] publicKeyHash) throws Exception {

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
                    if (txInput.canUnlockOutputWithKey(publicKeyHash)) {
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
     * @param
     * @return
     * @throws Exception
     */
    private Transaction[] findUnspentTransactions(byte[] publicKeyHash) throws Exception {
        // 获取地址对应所有已被花费了的交易输出，即被交易输入所指向的所有交易输出
        Map<String, int[]> allSpentTXOs = this.getAllSpentTXOs(publicKeyHash);
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
                    if (transaction.getOutputs()[i].canBeUnlockedWithKey(publicKeyHash)) {
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
     * @param
     * @return
     */
    public TXOutput[] findUTXO(byte[] publicKeyHash) throws Exception {
        Transaction[] unspentTxs = this.findUnspentTransactions(publicKeyHash);
        TXOutput[] utxos = {};
        if (unspentTxs == null || unspentTxs.length == 0) {
            return utxos;
        }
        for (Transaction tx : unspentTxs) {
            for (TXOutput txOutput : tx.getOutputs()) {
                if (txOutput.canBeUnlockedWithKey(publicKeyHash)) {
                    utxos = ArrayUtils.add(utxos, txOutput);
                }
            }
        }
        return utxos;
    }

    /**
     * 查询钱包余额，需要回顾所有的UTXO并且相加
     *
     * @param
     */
    private void getBalance(String address) throws Exception {
        // 检查钱包地址是否合法
        try {
            Base58Util.base58ToBytes(address);
        } catch (Exception e) {
            System.out.println("ERROR: invalid wallet address");
            throw new RuntimeException("ERROR: invalid wallet address", e);
        }
        // 得到公钥Hash值
        byte[] versionedPayload = Base58Util.base58ToBytes(address);
        byte[] publicKeyHash = Arrays.copyOfRange(versionedPayload, 1, versionedPayload.length);

        Blockchain blockchain = Blockchain.newBlockchain(address);
        TXOutput[] txOutputs = blockchain.findUTXO(publicKeyHash);
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
     * @param publicKeyHash
     * @param amount
     * @return
     * @throws Exception
     */
    public SpendableTXOutput findSpendableTXOutputs(byte[] publicKeyHash, int amount) throws Exception {
        //首先找到所有未花费的交易
        Transaction[] unspentTXs = this.findUnspentTransactions(publicKeyHash);
        int total = 0;
        Map<String, int[]> unspentTXOs = new HashMap<>();
        for (Transaction tx : unspentTXs) {
            String txId = Hex.encodeHexString(tx.getTxId());
            //遍历所有交易输出
            for (int i = 0; i < tx.getOutputs().length; i++) {
                TXOutput txOutput = tx.getOutputs()[i];
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

    public static void main(String[] args) {
        try {
            /*Wallet wallet = WalletUtil.getInstance().createWallet();
            System.out.println("wallet address : " + wallet.getBTCAddress());
            Wallet wallet1 = WalletUtil.getInstance().createWallet();
            System.out.println("wallet address : " + wallet1.getBTCAddress());
            Wallet wallet2 = WalletUtil.getInstance().createWallet();
            System.out.println("wallet address : " + wallet2.getBTCAddress());*/
            Blockchain blockchain = Blockchain.newBlockchain("1GvsHC3QAogGVS52QAabz8W8M5UVJsfgAe");

            /*wallet address : 1GvsHC3QAogGVS52QAabz8W8M5UVJsfgAe
            wallet address : 16VvVLZh4PLFV1cBWunRw2cmVmwA28RTE6
            wallet address : 1JpHt562Y5Gg2iZpqAwzaBSYc5hYNpJrYd*/

            //Blockchain blockchain = Blockchain.newBlockchain("yxz");
            //blockchain.getBalance("12LwyV25ooWb8AmQJYqGH4i1t2LGEgPJZE");
            //System.out.println(WalletUtil.getInstance().getWallet("1F3M2pwAGaBLWxw7wzKVqmqHqEZnB9yv6V"));
            //System.out.println(LevelDBUtil.getInstance().getLastBlockHash());

            /*Transaction transaction = Transaction.newTransaction("1GvsHC3QAogGVS52QAabz8W8M5UVJsfgAe", "16VvVLZh4PLFV1cBWunRw2cmVmwA28RTE6", 5, blockchain);
            blockchain.mineBlock(new Transaction[]{transaction});*/

            Transaction transaction1 = Transaction.newTransaction("1GvsHC3QAogGVS52QAabz8W8M5UVJsfgAe", "1JpHt562Y5Gg2iZpqAwzaBSYc5hYNpJrYd", 4, blockchain);
            blockchain.mineBlock(new Transaction[]{transaction1});
            //System.out.println("Success!");

            blockchain.getBalance("1GvsHC3QAogGVS52QAabz8W8M5UVJsfgAe");
            blockchain.getBalance("16VvVLZh4PLFV1cBWunRw2cmVmwA28RTE6");
            blockchain.getBalance("1JpHt562Y5Gg2iZpqAwzaBSYc5hYNpJrYd");
            LevelDBUtil.getInstance().closeDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
