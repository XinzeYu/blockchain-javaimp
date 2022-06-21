package com.yxz.block;

import com.yxz.consensus.ProofOfWork;
import com.yxz.util.LevelDBUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;

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


    public static Blockchain newBlockchain() {
        String lastBlockHash = LevelDBUtil.getInstance().getLastBlockHash();
        if (StringUtils.isBlank(lastBlockHash)) {
            Block genesisBlock = Block.newGenesisBlock();
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
         * 返回区块
         *
         * @return
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

    public static void main(String[] args) {
        try {
            /*Blockchain blockchain = Blockchain.newBlockchain();

            blockchain.addBlock("Send 1.0 BTC to wangwei");
            blockchain.addBlock("Send 2.5 more BTC to wangwei");
            blockchain.addBlock("Send 3.5 more BTC to wangwei");

            for (Blockchain.BlockchainIterator iterator = blockchain.getBlockchainIterator(); iterator.hashNext(); ) {
                Block block = iterator.next();

                if (block != null) {
                    boolean validate = ProofOfWork.newProofOfWork(block).validate();
                    System.out.println(block.toString() + ", validate = " + validate);
                }
            }*/
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
