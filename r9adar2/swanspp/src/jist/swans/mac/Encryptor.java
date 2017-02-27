package jist.swans.mac;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.Key;
 


import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
 


import org.apache.commons.lang3.SerializationUtils;

import jist.swans.misc.Message;
 
public class Encryptor {
 
    private static String keyStr ="RTcCQar3pUikoGVI";
 
    private static Key aesKey = null;
    private static Cipher cipher = null;
 
    synchronized private static void init() throws Exception {
        if (keyStr == null || keyStr.length() != 16) {
            throw new Exception("bad aes key configured");
        }
        if (aesKey == null) {
            aesKey = new SecretKeySpec(keyStr.getBytes(), "AES");
            cipher = Cipher.getInstance("AES");
        }
    }
 
    synchronized public static byte[] encrypt(Message msg) throws Exception {
        init();
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] array =convertToBytes(msg);
        return cipher.doFinal(array);
    }
  private static byte[] convertToBytes(Message object) throws IOException {
     /*   try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        }*/
        return SerializationUtils.serialize((Serializable) object); 
    }
 
    private static Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        /*try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                ObjectInput in = new ObjectInputStream(bis)) {
            return in.readObject();
        } */
    	return SerializationUtils.deserialize(bytes);
    }

    synchronized public static Message decrypt(byte[] array) throws Exception {
        init();
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return (Message) convertFromBytes(  cipher.doFinal(array));
    }
 
    public static String toHexString(byte[] array) {
        return DatatypeConverter.printHexBinary(array);
    }
 
    public static byte[] toByteArray(String s) {
        return DatatypeConverter.parseHexBinary(s);
    }
}