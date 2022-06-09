package com.yxz.block;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class Blockchain {

    /**
     * 区块链本质是一个块与块相连的链式结构
     */
    private List<Block> blockList;

    public Blockchain(List<Block> blockList) {
        this.blockList = blockList;
    }

    public void addBlock(String data) {
        Block previousBlock = blockList.get(blockList.size() - 1);
        this.addBlock(Block.createNewBlock(previousBlock.getHash(), data));
    }

    public void addBlock(Block block) {
        this.blockList.add(block);
    }


    public static Blockchain newBlockchain() {
        List<Block> blocks = new LinkedList<>();
        blocks.add(Block.newGenesisBlock());
        return new Blockchain(blocks);
    }


    public boolean isVaildChain() {
        Block preBlock;
        Block curBlock;

        for (int i = 1; i < blockList.size(); i++) {
            curBlock = blockList.get(i);
            preBlock = blockList.get(i - 1);

            if(!preBlock.getHash().equals(curBlock.getPreHash()) ) {
                System.out.println("Previous Hashes not equal");
                return false;
            }
        }
        return true;
    }

}
