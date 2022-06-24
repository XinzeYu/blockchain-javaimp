package com.yxz.util;

import com.google.common.collect.Maps;
import com.yxz.block.Block;
import com.yxz.transaction.TXOutput;
import lombok.Data;
import lombok.SneakyThrows;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Data
public class LevelDBUtil {

    /**
     * 区块链数据leveldb文件
     */
    private static final String DB_FILE = "blockchain.db";

    /**
     * 区块元数据桶前缀
     */
    private static final String BLOCKS_BUCKET_KEY = "blocks";

    /**
     * 链状态桶前缀
     */
    private static final String CHAINSTATE_BUCKET_KEY = "chainstate";

    /**
     * 获取最新一个区块的哈希值的键值，('l', lastblockhash)
     */
    private static final String LAST_BLOCK_KEY = "l";


    private volatile static LevelDBUtil instance;


    private DB db;

    /**
     * block buckets
     */
    private Map<String, byte[]> blocksBucket;

    /**
     * chainstate buckets
     */
    private Map<String, byte[]> chainstateBucket;


    public static LevelDBUtil getInstance() {
        if (instance == null) {
            synchronized (LevelDBUtil.class) {
                if (instance == null) {
                    instance = new LevelDBUtil();
                }
            }
        }
        return instance;
    }

    @SneakyThrows
    private LevelDBUtil() {
        openDB();
        initBlockBucket();
        initChainStateBucket();
    }

    /**
     * 打开数据库
     */
    private void openDB() {
        try {
            DBFactory factory = new Iq80DBFactory();
            Options options = new Options();
            db = factory.open(new File(DB_FILE), options);
        } catch (IOException e) {
            throw new RuntimeException("Fail to open db ! ", e);
        }
    }

    /**
     * 初始化 blocks 数据桶
     */
    private void initBlockBucket() {
        byte[] blockBucketKey = SerializeUtil.serialize(BLOCKS_BUCKET_KEY);
        byte[] blockBucketBytes = db.get(blockBucketKey);
        if (blockBucketBytes != null) {
            blocksBucket = (Map) SerializeUtil.deserialize(blockBucketBytes);
        } else {
            blocksBucket = Maps.newHashMap();
            db.put(blockBucketKey, SerializeUtil.serialize(blocksBucket));
        }
    }

    /**
     * 初始化链状态数据桶
     *
     */
    private void initChainStateBucket() {
        byte[] chainstateBucketKey = SerializeUtil.serialize(CHAINSTATE_BUCKET_KEY);
        byte[] chainstateBucketBytes = db.get(chainstateBucketKey);
        if (chainstateBucketBytes != null) {
            chainstateBucket = (Map) SerializeUtil.deserialize(chainstateBucketBytes);
        } else {
            chainstateBucket = Maps.newHashMap();
            db.put(chainstateBucketKey, SerializeUtil.serialize(chainstateBucket));
        }
    }


    /**
     * 保存最新一个区块的Hash值
     *
     * @param topBlockHash
     */
    public void putLastBlockHash(String topBlockHash) {
        blocksBucket.put(LAST_BLOCK_KEY, SerializeUtil.serialize(topBlockHash));
        db.put(SerializeUtil.serialize(BLOCKS_BUCKET_KEY), SerializeUtil.serialize(blocksBucket));

    }

    /**
     * 查询最新一个区块的Hash值
     *
     * @return
     */
    public String getLastBlockHash() {
        byte[] lastBlockHashBytes = blocksBucket.get(LAST_BLOCK_KEY);
        if (lastBlockHashBytes != null) {
            return (String) SerializeUtil.deserialize(lastBlockHashBytes);
        }
        return "";
    }

    /**
     * 保存区块
     *
     * @param block
     */
    public void putBlock(Block block) {
        blocksBucket.put(block.getHash(), SerializeUtil.serialize(block));
        db.put(SerializeUtil.serialize(BLOCKS_BUCKET_KEY), SerializeUtil.serialize(blocksBucket));
    }

    /**
     * 查询区块
     *
     * @param blockHash
     * @return
     */
    public Block getBlock(String blockHash) {
        return (Block) SerializeUtil.deserialize(blocksBucket.get(blockHash));
    }


    /**
     * 清空chainstate bucket
     */
    public void cleanChainStateBucket() {
        try {
            chainstateBucket.clear();
        } catch (Exception e) {
            System.out.println("Fail to clear chainstate bucket ! ");
            throw new RuntimeException("Fail to clear chainstate bucket ! ", e);
        }
    }

    /**
     * 保存UTXO数据
     *
     * @param key   交易ID
     * @param utxos UTXOs
     */
    public void putUTXOs(String key, TXOutput[] utxos) {
        try {
            chainstateBucket.put(key, SerializeUtil.serialize(utxos));
            db.put(SerializeUtil.serialize(CHAINSTATE_BUCKET_KEY), SerializeUtil.serialize(chainstateBucket));
        } catch (Exception e) {
            System.out.println("Fail to put UTXOs into chainstate bucket ! key=" + key);
            throw new RuntimeException("Fail to put UTXOs into chainstate bucket ! key=" + key, e);
        }
    }


    /**
     * 查询UTXO数据
     *
     * @param txId 交易ID
     */
    public TXOutput[] getUTXOs(String txId) {
        byte[] utxosByte = chainstateBucket.get(txId);
        if (utxosByte != null) {
            return (TXOutput[]) SerializeUtil.deserialize(utxosByte);
        }
        return null;
    }


    /**
     * 删除UTXO数据
     *
     * @param key 交易ID
     */
    public void deleteUTXOs(String key) {
        try {
            chainstateBucket.remove(key);
            db.put(SerializeUtil.serialize(CHAINSTATE_BUCKET_KEY), SerializeUtil.serialize(chainstateBucket));
        } catch (Exception e) {
            System.out.println("Fail to delete UTXOs by key ! key=" + key);
            throw new RuntimeException("Fail to delete UTXOs by key ! key=" + key, e);
        }
    }


    /**
     * 关闭数据库
     */
    public void closeDB() {
        try {
            db.close();
        } catch (Exception e) {
            throw new RuntimeException("Fail to close db ! ", e);
        }
    }

}
