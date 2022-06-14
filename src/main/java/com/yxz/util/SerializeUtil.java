package com.yxz.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class SerializeUtil {

    /**
     * 反序列化
     *
     * @param bytes 对象对应的byte数组
     * @return
     */
    public static Object deserialize(byte[] bytes) {
        Input input = new Input(bytes);
        Kryo kryo = new Kryo();
        //新版本的kryo避免Class is not registered错误
        kryo.setRegistrationRequired(false);
        Object obj = kryo.readClassAndObject(input);
        input.close();
        return obj;
    }

    /**
     * 序列化
     *
     * @param object 进行序列化的对象
     * @return
     */
    public static byte[] serialize(Object object) {
        Output output = new Output(4096, -1);
        Kryo kryo = new Kryo();
        //新版本的kryo避免Class is not registered错误
        kryo.setRegistrationRequired(false);
        kryo.writeClassAndObject(output, object);
        byte[] bytes = output.toBytes();
        output.close();
        return bytes;
    }
}
