package com.yxz.transaction;

import com.yxz.block.Blockchain;
import com.yxz.util.AddressUtil;
import com.yxz.util.SerializeUtil;
import com.yxz.util.WalletUtil;
import com.yxz.wallet.Wallet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import java.lang.invoke.LambdaMetafactory;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;


/**
 * 关于比特币的UTXO模型，实际上是交易输入与交易输出的结合：
 * 1. 有些交易输出并不是由交易输入产生，而是凭空产生的。
 * 2. 但交易输入必须指向某个交易输出，它不能凭空产生。
 * 3. 在一笔交易里面，交易输入可能会来自多笔交易所产生的交易输出。
 * 4. output跟input总数要吻合，实际上在交易的时候，并不可能刚刚好总是找到两笔加起来等于你要转出金额的output，
 * 如果Alice只想转4.5个BTC给Bob，但她只有5BTC的input，那么他就要多加一栏的output，把多出来的0.5个BTC转给自己，这样的交易才是平衡的。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    /**
     * 挖矿奖励数量
     */
    private static final int bonus = 10;

    /**
     * 签名算法
     */
    private static final String ALGORITHM = "SHA256withECDSA";

    /**
     * 交易的Hash值
     */
    private byte[] txId;

    /**
     * 交易输入
     */
    private TXInput[] inputs;

    /**
     * 交易输出
     */
    private TXOutput[] outputs;



    /**
     * 计算交易信息的Hash值
     *
     * @return
     */
    public byte[] hash() {
        byte[] serializeBytes = SerializeUtil.serialize(this);
        Transaction copyTx = (Transaction) SerializeUtil.deserialize(serializeBytes);
        copyTx.setTxId(new byte[]{});
        return DigestUtils.sha256(SerializeUtil.serialize(copyTx));
    }

    /**
     * 创建CoinBase交易，初始交易模块
     *
     * @param to   收账的钱包地址
     * @param data 解锁脚本数据
     * @return
     */
    public static Transaction newCoinbaseTX(String to, String data) {
        if (StringUtils.isBlank(data)) {
            data = String.format("Reward to '%s'", to);
        }
        // 创建交易输入
        TXInput txInput = new TXInput(new byte[]{}, -1, null, data.getBytes());

        // 创建交易输出，提供一笔激励
        TXOutput txOutput = TXOutput.newTXOutput(bonus, to);

        // 创建交易
        Transaction tx = new Transaction(null, new TXInput[]{txInput}, new TXOutput[]{txOutput});

        // 设置交易ID
        tx.setTxId(tx.hash());
        return tx;
    }

    /**
     * 判断是否为coinbase交易，交易输入对应一个TXInput txInput = new TXInput(new byte[]{}, -1, data);
     *
     * @return
     */
    public boolean isCoinbase() {
        return this.getInputs().length == 1
                && this.getInputs()[0].getTxId().length == 0
                && this.getInputs()[0].getTxOutputIndex() == -1;
    }


    public static Transaction newTransaction (String from, String to, int amount, Blockchain blockchain) throws Exception {
        //禁止自己与自己交易
        if (from.equals(to)) {
            System.out.println("ERROR: Prohibit oneself from trading with oneself!");
            throw new Exception("ERROR: Prohibit oneself from trading with oneself!");
        }

        //交易金额为0是没有意义的
        if (amount == 0) {
            System.out.println("ERROR: Transferring 0 is meaningless!");
            throw new Exception("ERROR: Transferring 0 is meaningless!");
        }

        //获取发送方的钱包信息
        Wallet senderWallet = WalletUtil.getInstance().getWallet(from);
        byte[] publicKey = senderWallet.getPublicKey();
        byte[] publicKeyHash = AddressUtil.ripeMD160Hash(publicKey);

        //需要发款方寻找能够花费的交易
        SpendableTXOutput spendableTXOutputs = blockchain.findSpendableTXOutputs(publicKeyHash, amount);
        int total = spendableTXOutputs.getTotal();
        Map<String, int[]> unspentTXOs = spendableTXOutputs.getUnspentTXOs();

        //余额不足
        if (total < amount) {
            System.out.println("Not enough funds");
            throw new Exception("ERROR: Not enough funds");
        }

        Iterator<Map.Entry<String, int[]>> iterator = unspentTXOs.entrySet().iterator();
        TXInput[] txInputs = {};
        //为每个找到的输出创建一个引用它的输入
        while (iterator.hasNext()) {
            Map.Entry<String, int[]> entry = iterator.next();
            String txIdStr = entry.getKey();
            int[] outIdxs = entry.getValue();
            byte[] txId = Hex.decodeHex(txIdStr);
            for (int outIndex : outIdxs) {
                txInputs = ArrayUtils.add(txInputs, new TXInput(txId, outIndex, null, publicKey));
            }
        }

        //而后创建交易输出
        //一个 output 用于锁定到收款的钱包地址上，是真正转账的金额；
        //另一个则用于给自己找零
        TXOutput[] txOutput = {};
        txOutput = ArrayUtils.add(txOutput, TXOutput.newTXOutput(amount, to));
        //给自己找零
        if (total > amount) {
            txOutput = ArrayUtils.add(txOutput, TXOutput.newTXOutput((total - amount), from));
        }

        Transaction tx = new Transaction(null, txInputs, txOutput);
        tx.setTxId(tx.hash());

        //利用from的私钥对该交易进行签名
        blockchain.signTransaction(tx, senderWallet.getPrivateKey());
        return tx;
    }



    /**
     * 创建用于签名的交易数据副本
     * 其中交易输入的signature和publicKey需要设置为null
     *
     * @return
     */
    public Transaction trimmedCopy() {
        TXInput[] tmpTXInputs = new TXInput[this.getInputs().length];
        for (int i = 0; i < this.getInputs().length; i++) {
            TXInput txInput = this.getInputs()[i];
            tmpTXInputs[i] = new TXInput(txInput.getTxId(), txInput.getTxOutputIndex(), null, null);
        }

        TXOutput[] tmpTXOutputs = new TXOutput[this.getOutputs().length];
        for (int i = 0; i < this.getOutputs().length; i++) {
            TXOutput txOutput = this.getOutputs()[i];
            tmpTXOutputs[i] = new TXOutput(txOutput.getValue(), txOutput.getPublicKeyHash());
        }

        return new Transaction(this.getTxId(), tmpTXInputs, tmpTXOutputs);
    }


    /**
     *
     * 签名
     *
     *
     * @param privateKey 用于签名的私钥
     * @param prevTX
     * @throws Exception
     */
    public void sign(BCECPrivateKey privateKey, Map<String, Transaction> prevTX) throws Exception {
        //coinbase交易信息不需要签名
        if (this.isCoinbase()) {
            return;
        }
        //验证一下交易信息中的交易输入是否正确，也就是能否查找对应的交易数据
        for (TXInput txInput : this.getInputs()) {
            if (prevTX.get(Hex.encodeHexString(txInput.getTxId())) == null) {
                throw new RuntimeException("ERROR: Previous transaction is not correct");
            }
        }

        //创建用于签名的交易信息的副本
        Transaction txCopy = this.trimmedCopy();

        Security.addProvider(new BouncyCastleProvider());
        Signature ecdsaSign = Signature.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
        ecdsaSign.initSign(privateKey);

        for (int i = 0; i < txCopy.getInputs().length; i++) {
            TXInput txInputCopy = txCopy.getInputs()[i];
            // 获取交易输入TxID对应的交易数据
            Transaction prevTx = prevTX.get(Hex.encodeHexString(txInputCopy.getTxId()));
            // 获取交易输入所对应的上一笔交易中的交易输出
            TXOutput prevTxOutput = prevTx.getOutputs()[txInputCopy.getTxOutputIndex()];
            txInputCopy.setPublicKey(prevTxOutput.getPublicKeyHash());
            txInputCopy.setSignature(null);
            // 得到要签名的数据，即交易ID
            txCopy.setTxId(txCopy.hash());
            txInputCopy.setPublicKey(null);

            // 对整个交易信息仅进行签名，即对交易ID进行签名
            ecdsaSign.update(txCopy.getTxId());
            byte[] signature = ecdsaSign.sign();

            // 将整个交易数据的签名赋值给交易输入，因为交易输入需要包含整个交易信息的签名
            // 注意是将得到的签名赋值给原交易信息中的交易输入
            this.getInputs()[i].setSignature(signature);
        }

    }

    /**
     * 验证交易信息
     *
     *
     *
     * @param prevTX 前面多笔交易集合
     * @return
     */
    public boolean verify(Map<String, Transaction> prevTX) throws Exception {
        // coinbase 交易信息不需要签名，也就无需验证
        if (this.isCoinbase()) {
            return true;
        }

        // 再次验证一下交易信息中的交易输入是否正确，也就是能否查找对应的交易数据
        for (TXInput txInput : this.getInputs()) {
            if (prevTX.get(Hex.encodeHexString(txInput.getTxId())) == null) {
                throw new Exception("ERROR: Previous transaction is not correct");
            }
        }

        // 创建用于签名验证的交易信息的副本
        Transaction txCopy = this.trimmedCopy();

        Security.addProvider(new BouncyCastleProvider());
        ECParameterSpec ecParameters = ECNamedCurveTable.getParameterSpec("secp256k1");
        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
        Signature ecdsaVerify = Signature.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);

        for (int i = 0; i < this.getInputs().length; i++) {
            TXInput txInput = this.getInputs()[i];
            //获取交易输入TxID对应的交易数据
            Transaction prevTx = prevTX.get(Hex.encodeHexString(txInput.getTxId()));
            //获取交易输入所对应的上一笔交易中的交易输出
            TXOutput prevTxOutput = prevTx.getOutputs()[txInput.getTxOutputIndex()];

            TXInput txInputCopy = txCopy.getInputs()[i];
            txInputCopy.setSignature(null);
            txInputCopy.setPublicKey(prevTxOutput.getPublicKeyHash());
            //得到要签名的数据，即交易ID
            txCopy.setTxId(txCopy.hash());
            txInputCopy.setPublicKey(null);

            //使用交易输入中的椭圆曲线公钥点对（x,y）
            BigInteger x = new BigInteger(1, Arrays.copyOfRange(txInput.getPublicKey(), 1, 33));
            BigInteger y = new BigInteger(1, Arrays.copyOfRange(txInput.getPublicKey(), 33, 65));
            ECPoint ecPoint = ecParameters.getCurve().createPoint(x, y);

            //根据点坐标对生成publicKey
            ECPublicKeySpec keySpec = new ECPublicKeySpec(ecPoint, ecParameters);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(txCopy.getTxId());
            if (!ecdsaVerify.verify(txInput.getSignature())) {
                return false;
            }
        }
        return true;
    }

}
