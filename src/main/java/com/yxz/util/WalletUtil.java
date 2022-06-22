package com.yxz.util;


import com.google.common.collect.Maps;
import com.yxz.wallet.Wallet;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.Security;
import java.util.Map;
import java.util.Set;

/**
 * 钱包工具类，用于创建钱包等等功能
 *
 *
 */
public class WalletUtil {
    /**
     * 钱包工具实例
     */
    private volatile static WalletUtil instance;

    /**
     * 钱包文件
     */
    private final static String WALLET_FILE = "wallet.dat";

    /**
     * 对称加密算法
     */
    private static final String ALGORITHM = "AES";

    /**
     * 密文
     */
    private static final byte[] CIPHER_TEXT = "2oF@5sC%DNf32y!TmiZi!tG9W5rLaniD".getBytes();


    public static WalletUtil getInstance() {
        if (instance == null) {
            synchronized (WalletUtil.class) {
                if (instance == null) {
                    instance = new WalletUtil();
                }
            }
        }
        return instance;
    }

    private WalletUtil() {
        initWalletFile();
    }

    /**
     * 初始化钱包文件，钱包文件存在磁盘中
     */
    private void initWalletFile() {
        File file = new File(WALLET_FILE);
        if (!file.exists()) {
            this.saveToDisk(new Wallets());
        } else {
            this.loadFromDisk();
        }
    }

    /**
     * 获取所有的钱包地址
     *
     * @return
     */
    public Set<String> getAddresses() {
        Wallets wallets = this.loadFromDisk();
        return wallets.getAddresses();
    }

    /**
     * 获取钱包数据
     *
     * @param address 钱包地址
     * @return
     */
    public Wallet getWallet(String address) {
        Wallets wallets = this.loadFromDisk();
        return wallets.getWallet(address);
    }

    /**
     * 创建钱包，新建后加入到wallets中，存储到磁盘上
     *
     * @return
     */
    public Wallet createWallet() {
        Wallet wallet = new Wallet();
        Wallets wallets = this.loadFromDisk();
        wallets.addWallet(wallet);
        this.saveToDisk(wallets);
        return wallet;
    }

    /**
     * 存储到磁盘文件中
     *
     * @param wallets
     */
    private void saveToDisk(Wallets wallets) {
        try {
            if (wallets == null) {
                System.out.println("Fail to save wallet to file ! wallets is null ");
                throw new Exception("ERROR: Fail to save wallet to file !");
            }
            SecretKeySpec sks = new SecretKeySpec(CIPHER_TEXT, ALGORITHM);
            Security.setProperty("crypto.policy", "unlimited");
            // 生成密文
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, sks);
            SealedObject sealedObject = new SealedObject(wallets, cipher);

            // 自动回收机制
            @Cleanup CipherOutputStream cos = new CipherOutputStream(
                    new BufferedOutputStream(new FileOutputStream(WALLET_FILE)), cipher);
            @Cleanup ObjectOutputStream outputStream = new ObjectOutputStream(cos);
            outputStream.writeObject(sealedObject);
        } catch (Exception e) {
            System.out.println("Fail to save wallet to disk !");
            throw new RuntimeException("Fail to save wallet to disk !");
        }
    }

    /**
     * 加载钱包数据
     */
    private Wallets loadFromDisk() {
        try {
            SecretKeySpec sks = new SecretKeySpec(CIPHER_TEXT, ALGORITHM);
            Security.setProperty("crypto.policy", "unlimited");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, sks);
            //自动回收机制
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(WALLET_FILE));
            while (bufferedInputStream.available() == 0) return new Wallets();
            @Cleanup CipherInputStream cipherInputStream = new CipherInputStream(
                    bufferedInputStream, cipher);
            //ObjectInputStream inputStream = new ObjectInputStream();
            //while (cipherInputStream.available() != 0) {
                @Cleanup ObjectInputStream inputStream = new ObjectInputStream(cipherInputStream);
                SealedObject sealedObject = (SealedObject) inputStream.readObject();
                return (Wallets) sealedObject.getObject(cipher);
            //}
            //@Cleanup ObjectInputStream inputStream = new ObjectInputStream(cipherInputStream);
            //SealedObject sealedObject = (SealedObject) inputStream.readObject();
            //return (Wallets) sealedObject.getObject(cipher);
            //return new Wallets();
        } catch (Exception e) {
            System.out.println("Fail to load wallet from disk ! ");
            throw new RuntimeException("Fail to load wallet from disk ! ");
        }
    }

    /**
     * 静态内部类，钱包存储对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Wallets implements Serializable {

        private static final long serialVersionUID = -2542070981569243131L;

        private Map<String, Wallet> walletMap = Maps.newHashMap();

        /**
         * 添加钱包
         *
         * @param wallet
         */
        private void addWallet(Wallet wallet) {
            try {
                this.walletMap.put(wallet.getBTCAddress(), wallet);
            } catch (Exception e) {
                System.out.println("Fail to add wallet ! ");
                throw new RuntimeException("Fail to add wallet !");
            }
        }

        /**
         * 获取所有的钱包地址
         *
         * @return
         */
        Set<String> getAddresses() {
            if (walletMap == null) {
                System.out.println("Fail to get address ! walletMap is null ! ");
                throw new RuntimeException("Fail to get addresses ! ");
            }
            return walletMap.keySet();
        }

        /**
         * 获取钱包数据
         *
         * @param address 钱包地址
         * @return
         */
        Wallet getWallet(String address) {
            // 检查钱包地址是否合法
            try {
                Base58Util.base58ToBytes(address);
            } catch (Exception e) {
                System.out.println("Fail to get wallet ! address invalid ! address=");
                throw new RuntimeException("Fail to get wallet ! ");
            }
            Wallet wallet = walletMap.get(address);
            if (wallet == null) {
                System.out.println("Fail to get wallet ! wallet don`t exist ! address=");
                throw new RuntimeException("Fail to get wallet ! ");
            }
            return wallet;
        }
    }

}
