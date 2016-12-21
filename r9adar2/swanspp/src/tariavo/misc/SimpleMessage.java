package tariavo.misc;

import java.util.Arrays;

import jist.swans.misc.Message;
/**
 * simple message class
 * */
public class SimpleMessage implements Message {
	public String getMessageID()
	  {
		  return "not implemented";
	  }
	
	private byte[] arr = null;
	
	public SimpleMessage(byte[] arr) {
		if(arr == null) throw new RuntimeException();
		this.arr = new byte[arr.length];
		System.arraycopy(arr, 0, this.arr, 0, arr.length);
	}
	
	public SimpleMessage(String str) {
		this.arr = str.getBytes();
	}

	public int getSize() {
		return this.arr.length;
	}

	public void getBytes(byte[] msg, int offset) {
		System.arraycopy(this.arr, 0, msg, offset, this.arr.length);
	}
	
	public String toString() {
		return arr == null ? "null" : new String(arr);
	}
	
	public boolean equals(Object o) {
		if(o instanceof SimpleMessage &&
				Arrays.equals(arr, ((SimpleMessage)o).arr))
			return true;
		return false;
	}
}
