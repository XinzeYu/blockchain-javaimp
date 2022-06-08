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

}
