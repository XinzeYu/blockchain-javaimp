package com.yxz.wallet;

import com.yxz.util.AddressUtil;
import com.yxz.util.Base58Util;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.*;


/**
 * 比特币的钱包以及地址生成步骤：
 * 1. 随机选取32个字节作为私钥，然后用非对称加密算法，即SECP256k1椭圆曲线计算，得出一个公钥数值;
 * 2. 对该公钥数值进行一次SHA-256计算，得到一个哈希公钥数值;
 * 3. 对上面这个哈希公钥数值进行一次RIPEMD-160算法，得到新的哈希公钥数值;
 * 4. 对这个新的哈希公钥数值的前面添加系统的版本号;
 * 5. 这个有系统版本号的数值进行两次次SHA-256计算（生成校验码）;
 * 6. 取出最新哈希公钥数值的前4个字节;
 * 7. 把这个4个字节加在第四步有版本号的数值的后面（Base58Check方式的校验码）;
 * 8. Base58编码方式把上一步结果生成最终地址。
 *
 */
@Data
@AllArgsConstructor
public class Wallet implements Serializable {

    private static final long serialVersionUID = 166249065006236265L;

    // 校验码长度
    private static final int ADDRESS_CHECKSUM_LEN = 4;
    /**
     * 私钥
     */
    private BCECPrivateKey privateKey;
    /**
     * 公钥
     */
    private byte[] publicKey;

    public Wallet() {
        initWallet();
    }

    /**
     * 初始化钱包
     */
    private void initWallet() {
        try {
            KeyPair keyPair = newECKeyPair();
            BCECPrivateKey privateKey = (BCECPrivateKey) keyPair.getPrivate();
            BCECPublicKey publicKey = (BCECPublicKey) keyPair.getPublic();
        
            byte[] publicKeyBytes = publicKey.getQ().getEncoded(false);

            this.setPrivateKey(privateKey);
            this.setPublicKey(publicKeyBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private KeyPair newECKeyPair() throws InvalidAlgorithmParameterException, NoSuchProviderException, NoSuchAlgorithmException {
        // 注册 BC Provider
        Security.addProvider(new BouncyCastleProvider());

        // 创建椭圆曲线算法的密钥对生成器，算法为 ECDSA
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
        // 椭圆曲线（EC）域参数设定，选择secp256k1
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        keyPairGenerator.initialize(ecSpec, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * 获取钱包地址，钱包地址由公钥单向生成
     *
     * @return
     */
    public String getBTCAddress() {
        try {
            // 1.获取经过SHA-256以及RIPEMD-160算法的哈希公钥数值
            byte[] ripemdHashedKey = AddressUtil.ripeMD160Hash(this.getPublicKey());

            // 2.添加版本号这里默认为00
            ByteArrayOutputStream addrStream = new ByteArrayOutputStream();
            addrStream.write((byte) 0);
            addrStream.write(ripemdHashedKey);
            byte[] versionedPayload = addrStream.toByteArray();

            // 3.计算校验码，取出前4个字节
            byte[] checksum = AddressUtil.checksum(versionedPayload);

            // 4.得到 version + paylod + checksum的组合
            addrStream.write(checksum);
            byte[] binaryAddress = addrStream.toByteArray();

            // 5.执行Base58转换处理
            return Base58Util.rawBytesToBase58(binaryAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Fail to get wallet address ! ");
    }

}
