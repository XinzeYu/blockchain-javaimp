package com.yxz.transaction;

import com.google.common.collect.Lists;
import com.yxz.util.ByteUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.List;


/**
 * 实现默克尔树数据结构，将一个区块的交易信息的表示成一个唯一的根hash
 *
 *
 */
@Data
public class MerkleTree {

    /**
     * 默克尔树的节点数据结构
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Node {
        private byte[] hash;
        private Node left;
        private Node right;
    }

    /**
     * 根节点
     */
    private Node root;


    /**
     * 叶子节点的Hash值
     */
    private byte[][] leafHashes;

    public MerkleTree(byte[][] leafHashes) {
        constructTree(leafHashes);
    }


    /**
     * 从底部叶子节点开始往上构建整个Merkle Tree
     *
     *
     * @param leafHashes
     */
    private void constructTree(byte[][] leafHashes) {
        if (leafHashes == null || leafHashes.length < 1) {
            throw new RuntimeException("ERROR:Fail to construct merkle tree ! leafHashes data invalid ! ");
        }
        this.leafHashes = leafHashes;
        //底部节点构建层
        List<Node> parents = bottomLevel(leafHashes);
        //不断向上构建
        while (parents.size() > 1) {
            parents = internalLevel(parents);
        }
        root = parents.get(0);
    }


    /**
     * 底部节点的构建函数
     *
     * @param leafHashes
     * @return
     */
    private List<Node> bottomLevel(byte[][] leafHashes) {
        //向上一层，节点数量减少一半
        List<Node> parents = Lists.newArrayListWithCapacity(leafHashes.length / 2);

        for (int i = 0; i < leafHashes.length - 1; i += 2) {
            Node leaf1 = constructLeafNode(leafHashes[i]);
            Node leaf2 = constructLeafNode(leafHashes[i + 1]);

            Node parent = constructInternalNode(leaf1, leaf2);
            parents.add(parent);
        }

        //merkle树在奇数个节点的情况，需要复制最后一个节点再向上构建
        if (leafHashes.length % 2 != 0) {
            Node leaf = constructLeafNode(leafHashes[leafHashes.length - 1]);
            Node parent = constructInternalNode(leaf, leaf);
            parents.add(parent);
        }

        return parents;
    }

    /**
     * 构建一个层级节点
     *
     * @param children
     * @return
     */
    private List<Node> internalLevel(List<Node> children) {
        //向上一层，节点数量减少一半
        List<Node> parents = Lists.newArrayListWithCapacity(children.size() / 2);

        for (int i = 0; i < children.size() - 1; i += 2) {
            Node child1 = children.get(i);
            Node child2 = children.get(i + 1);

            Node parent = constructInternalNode(child1, child2);
            parents.add(parent);
        }

        //若节点剩出来一个，只对left节点进行计算
        if (children.size() % 2 != 0) {
            Node child = children.get(children.size() - 1);
            Node parent = constructInternalNode(child, null);
            parents.add(parent);
        }

        return parents;
    }


    /**
     * 构建叶子节点，叶子节点只有hash值
     *
     * @param hash
     * @return
     */
    private static Node constructLeafNode(byte[] hash) {
        Node leaf = new Node();
        leaf.hash = hash;
        return leaf;
    }

    /**
     * 构建内部节点
     *
     * @param leftChild
     * @param rightChild
     * @return
     */
    private Node constructInternalNode(Node leftChild, Node rightChild) {
        Node parent = new Node();
        if (rightChild == null) {
            parent.hash = leftChild.hash;
        } else {
            parent.hash = internalHash(leftChild.hash, rightChild.hash);
        }
        parent.left = leftChild;
        parent.right = rightChild;
        return parent;
    }


    /**
     * 计算内部节点Hash
     *
     * @param leftChildHash
     * @param rightChildHash
     * @return
     */
    private byte[] internalHash(byte[] leftChildHash, byte[] rightChildHash) {
        byte[] mergedBytes = ByteUtil.merge(leftChildHash, rightChildHash);
        return DigestUtils.sha256(mergedBytes);
    }

}
