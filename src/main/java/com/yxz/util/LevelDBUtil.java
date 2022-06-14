package com.yxz.util;

import com.google.common.collect.Maps;
import com.yxz.block.Block;
import lombok.SneakyThrows;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

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
     * 获取最新一个区块的哈希值的键值，('l', lastblockhash)
     */
    private static final String LAST_BLOCK_KEY = "l";


    private volatile static LevelDBUtil instance;


    private DB db;

    /**
     * block buckets
     */
    private Map<String, byte[]> blocksBucket;

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
     * 保存最新一个区块的Hash值
     *
     * @param tipBlockHash
     */
    public void putLastBlockHash(String tipBlockHash) {
        blocksBucket.put(LAST_BLOCK_KEY, SerializeUtil.serialize(tipBlockHash));
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
