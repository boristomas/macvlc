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

import jist.runtime.JistAPI;
import jist.swans.Constants;
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
        JistAPI.sleep(Constants.MICRO_SECOND * 12);//http://www.cse.wustl.edu/~jain/cse567-06/ftp/encryption_perf/
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] array =convertToBytes(msg);
        try
        {
        return cipher.doFinal(array);
        }
  	  catch(Exception ex)
  	  {
  		  return null;
  	  }
    }
  private static byte[] convertToBytes(Message object) throws IOException {
     /*   try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        }*/
	  try{
        return SerializationUtils.serialize((Serializable) object); 
	  }
	  catch(Exception ex)
	  {
		  return null;
	  }
    }
 
    private static Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        /*try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                ObjectInput in = new ObjectInputStream(bis)) {
            return in.readObject();
        } */
    	try
    	{
    	return SerializationUtils.deserialize(bytes);
    	}
  	  catch(Exception ex)
  	  {
  		  return null;
  	  }
    }

    synchronized public static Message decrypt(byte[] array) throws Exception {
        init();
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        try
        {
        return (Message) convertFromBytes(  cipher.doFinal(array));
        }
  	  catch(Exception ex)
  	  {
  		  return null;
  	  }
    }
 
    public static String toHexString(byte[] array) {
        return DatatypeConverter.printHexBinary(array);
    }
 
    public static byte[] toByteArray(String s) {
        return DatatypeConverter.parseHexBinary(s);
    }
}