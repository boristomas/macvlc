//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Pickle.java Tue 2004/04/06 11:46:44 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Vector;

import jist.runtime.JistAPI;
import jist.swans.net.NetAddress;
import sun.misc.HexDumpEncoder;

/**
 * Utility class to simplify the serialization and deserialization
 * of various data types.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Pickle.java,v 1.1 2007/04/09 18:49:20 drchoffnes Exp $
 * @since SWANS1.0
 */

public final class Pickle 
{
	
	// Interface for Message objects to implement,
	// which defines class static final arrays of fields to serialize
	public interface Serializable extends JistAPI.DoNotRewrite {
		
		// Fields can't be required in interfaces...
		// this is just a sample prototype for what all classes that implement
		// this method must define.
		public final String[] fieldNames = {};
		
	}

	public class Deserialized {
		
		public Object o;
		public int bytes;
		
		public Deserialized(Object o, int bytes) {
			this.o = o;
			this.bytes = bytes;
		}
		
	}
	
	
	// to implement:
	// double
	
	//
	// Begin Functions by John Otto for serializing complex classes
	//
	
	public static byte[] serialize(Object o) throws Exception {
		
		byte[] toReturn = new byte[0]; 
		
		if(o != null && o.getClass().getName().equals("[Ljist.swans.net.NetAddress;")) {
			Class c = o.getClass();
			boolean b = c.isArray();
			
			// match!
			int i = 0;
		}
		
		// see if there's any object to serialize
		if(o == null) {
			// if not, then simply output a '\0' character...this will be detected as a null indicator.
			// all other objects will start with a non-zero character
			toReturn = new byte[1];
			toReturn[0] = '\0';
			return toReturn;
			
		// Check if the object's class is a special case that we implement
		} else if(o.getClass().isArray()) {
			
			Class c = o.getClass();
			//Class c = o.getClass().getComponentType();
			
			ArrayList arrayBytes = new ArrayList();
			int count = 0;

			// write the number of elements
			int elements = Array.getLength(o);
			byte[] length = new byte[4];
			count += 4;
			integerToArray(elements, length, 0);
			arrayBytes.add(length);
			
			// write the class name of the elements
			byte[] className = Array.get(o, 0).getClass().getName().getBytes();
			byte[] classNameTerm = new byte[className.length + 1];
			System.arraycopy(className, 0, classNameTerm, 0, className.length);
			classNameTerm[className.length] = '\0';
			arrayBytes.add(classNameTerm);
			count += classNameTerm.length;
			
			
			// Add a byte for whether the underlying type is a primitive (1 = primitive; 0 = object) 
			byte[] primitive = new byte[1];
			if(c.getComponentType().isPrimitive()) {
				primitive[0] = 1;
			} else {
				primitive[0] = 0;
			}
			arrayBytes.add(primitive);
			count++;
			
			// write each individual element
			for(int i = 0; i < elements; i++) {
				byte[] objBytes = serialize(Array.get(o, i));
				arrayBytes.add(objBytes);
				count += objBytes.length;
			}
						
			toReturn = concatenateArrayList(arrayBytes, count);
		} else if(o.getClass() == Integer.class) {
			toReturn = new byte[4];
			integerToArray(((Integer) o).intValue(), toReturn, 0);
		} else if(o.getClass() == int.class) {
			toReturn = new byte[4];
			integerToArray(((Integer) o).intValue(), toReturn, 0);
		} else if(o.getClass() == Long.class) {
			byte[] longBytes = Long.toHexString(((Long) o).longValue()).getBytes();
			toReturn = new byte[longBytes.length + 4];
			integerToArray(longBytes.length + 4, toReturn, 0);
			System.arraycopy(longBytes, 0, toReturn, 4, longBytes.length);
		} else if(o.getClass() == Float.class) {
			toReturn = serialize(new Integer(Float.floatToIntBits(((Float) o).floatValue())));
		} else if(o.getClass() == Byte.class) {
			toReturn = new byte[1];
			toReturn[0] = ((Byte) o).byteValue();
		} else if(o.getClass() == Boolean.class) {
			toReturn = new byte[1];
			if(((Boolean) o).booleanValue() == true) toReturn[0] = '\0';
			else toReturn[0] = '\1';
		} else if(o.getClass() == Short.class) {
			toReturn = new byte[2];
			shortToArray(((Short) o).shortValue(), toReturn, 0);
		} else if(o.getClass() == String.class) {
			String str = (String) o;
			toReturn = new byte[str.getBytes().length + 4];
			integerToArray(toReturn.length, toReturn, 0);
			System.arraycopy(str.getBytes(), 0, toReturn, 4, str.getBytes().length);
		} else if(o.getClass() == NetAddress.class) {
			toReturn = new byte[4];
			integerToArray(((NetAddress) o).toInt(), toReturn, 0);
		} else if(o.getClass() == ArrayList.class) {
			ArrayList arrayBytes = new ArrayList();
			int count = 0;
			
			// write the number of elements
			int elements = ((ArrayList) o).size();
			byte[] length = new byte[4];
			count += 4;
			integerToArray(elements, length, 0);
			arrayBytes.add(length);
			
			// for each element
			for(int i = 0; i < elements; i++) {
				//write a null-terminated class name
				byte[] className = ((ArrayList) o).get(i).getClass().getName().getBytes();
				byte[] classNameTerm = new byte[className.length + 1];
				System.arraycopy(className, 0, classNameTerm, 0, className.length);
				classNameTerm[className.length] = '\0';
				arrayBytes.add(classNameTerm);
				count += classNameTerm.length;
				
				// then write the object
				byte[] objBytes = serialize(((ArrayList) o).get(i));
				arrayBytes.add(objBytes);
				count += objBytes.length;
			}
			
			toReturn = concatenateArrayList(arrayBytes, count);			
		} else if(o.getClass() == Vector.class) {
			ArrayList arrayBytes = new ArrayList();
			int count = 0;
			
			// write the number of elements
			int elements = ((Vector) o).size();
			byte[] length = new byte[4];
			count += 4;
			integerToArray(elements, length, 0);
			arrayBytes.add(length);
			
			// for each element
			for(int i = 0; i < elements; i++) {
				//write a null-terminated class name
				byte[] className = ((Vector) o).get(i).getClass().getName().getBytes();
				byte[] classNameTerm = new byte[className.length + 1];
				System.arraycopy(className, 0, classNameTerm, 0, className.length);
				classNameTerm[className.length] = '\0';
				arrayBytes.add(classNameTerm);
				count += classNameTerm.length;
				
				// then write the object
				byte[] objBytes = serialize(((Vector) o).get(i));
				arrayBytes.add(objBytes);
				count += objBytes.length;
			}
			
			toReturn = concatenateArrayList(arrayBytes, count);			
		} else if(o.getClass() == LinkedList.class) {
			LinkedList ll = (LinkedList) o;
			ArrayList listBytes = new ArrayList();
			int count = 0;
			byte[] length = new byte[4];
			count += 4;
			integerToArray(ll.size(), length, 0);
			listBytes.add(length);
			for(int j = 0; j < ll.size(); j++) {
				byte[] objBytes = serialize(ll.get(j));
				listBytes.add(objBytes);
				count += objBytes.length;
			}
			toReturn = concatenateArrayList(listBytes, count);
		} else {			
			if(!implementsOurSerializable(o.getClass())) {
				System.err.println("Class " + o.getClass().getName() + " cannot be serialized!");
				throw new Exception();
			}
		
			// Create an arraylist to hold all the bytes to be concatenated together
			ArrayList bytes = new ArrayList();
			int byteCount = 0;
			
			// Add space to store the byte count
			bytes.add(new byte[4]);
			byteCount += 4;
			
			// Add the null-terminated class name
			bytes.add(o.getClass().getName().getBytes());
			byteCount += o.getClass().getName().getBytes().length;
			bytes.add("\0".getBytes());
			byteCount += 1;
					
			// Iterate through the "fieldTypes"/"fieldNames" arrays
			// ...adding each piece of appropriate information
			String[] fieldNames = (String[]) o.getClass().getField("fieldNames").get(o);
			
			for(int i = 0; i < fieldNames.length; i++) {
				Object obj = o.getClass().getField(fieldNames[i]).get(o);
				byte[] result = serialize(obj);
				bytes.add(result);
				byteCount += result.length;
			}
			
			// Put the total length in the first element of the bytes arraylist
			integerToArray(byteCount, (byte[]) bytes.get(0), 0);
			
			// Do the actual concatenation of byte arrays.
			toReturn = concatenateArrayList(bytes, byteCount);	
		}
		
		byte[] nonNull = new byte[toReturn.length + 1];
		nonNull[0] = '\1';
		System.arraycopy(toReturn, 0, nonNull, 1, toReturn.length);
		return nonNull;
	}
	
	public static byte[] concatenateArrayList(ArrayList bytes, int byteCount) {
		byte[] bytesOut = new byte[byteCount];
		int currentByte = 0;
		
		// insert the rest of the fields
		for(int i = 0; i < bytes.size(); i++) {
			int length = ((byte[]) bytes.get(i)).length;
			System.arraycopy(bytes.get(i), 0, bytesOut, currentByte, length);
			currentByte += length;
		}
		return bytesOut;
	}
	
	public static boolean implementsOurSerializable(Class c) {
		boolean serializable = false;
		
		Class current = c;
		while(current != null) {
			Class[] interfaces = current.getInterfaces();
			for(int i = 0; i < interfaces.length; i++) {
				if(interfaces[i].equals(Pickle.Serializable.class)) {
					serializable = true;
					break;
				}
			}
			if(serializable) break;
			current = current.getSuperclass();
		}
		
		return serializable;
	}
	
	public static Deserialized deserialize(byte[] bytes, int offset, Class c) throws Exception {
				
		int bytesRead = 0;
		Object toReturn = new Object();
		
		// Check if the first byte is 0 or 1
		if(bytes[offset] == '\0') {
			return new Pickle().new Deserialized(null, 1);
		}
		
		// for the null char
		bytesRead++;
		offset++;
		
		// Check if the given class matches a special case
		if(c.isArray()) {
			// read the number of objects to deserialize
			int elements = arrayToInteger(bytes, offset);
			bytesRead += 4;
			
			// read the className of the elements
			int start = bytesRead - 1;
			while(bytes[offset + bytesRead] != '\0') {
				bytesRead++;
			}
			
			byte[] className = new byte[bytesRead - start];
			System.arraycopy(bytes, offset + 4, className, 0, bytesRead - start);
			String classString = new String(className);
			Class arrayClass = Class.forName(classString);
			
			bytesRead++; // for the null char.
			
			// read the next byte...if 1, then the underlying class is a primitive value; if 0, then read objects like normal
			byte b = bytes[offset + 4 + classString.length() + 1];
			bytesRead++;
			
			// instantiate the array
			if(b == 1) {
				if(arrayClass.equals(Boolean.class)) {
					toReturn = Array.newInstance(boolean.class, elements);
				} else if(arrayClass.equals(Byte.class)) {
					toReturn = Array.newInstance(byte.class, elements);
				} else if(arrayClass.equals(Character.class)) {
					toReturn = Array.newInstance(char.class, elements);
				} else if(arrayClass.equals(Short.class)) {
					toReturn = Array.newInstance(short.class, elements);
				} else if(arrayClass.equals(Integer.class)) {
					toReturn = Array.newInstance(int.class, elements);
				} else if(arrayClass.equals(Long.class)) {
					toReturn = Array.newInstance(long.class, elements);
				} else if(arrayClass.equals(Float.class)) {
					toReturn = Array.newInstance(float.class, elements);
				} else if(arrayClass.equals(Double.class)) {
					toReturn = Array.newInstance(double.class, elements);
				}
			} else {
				toReturn = Array.newInstance(arrayClass, elements);
			}
			
			// read the individual elements, deserialize each into an object
			for(int i = 0; i < elements; i++) {
				Deserialized ds = deserialize(bytes, offset + bytesRead, arrayClass);
				if(b == 1) {
					// primitive type...
					if(arrayClass.equals(Boolean.class)) {
						Array.setBoolean(toReturn, i, ((Boolean)ds.o).booleanValue());
					} else if(arrayClass.equals(Byte.class)) {
						Array.setByte(toReturn, i, ((Byte)ds.o).byteValue());
					} else if(arrayClass.equals(Character.class)) {
						Array.setChar(toReturn, i, ((Character)ds.o).charValue());
					} else if(arrayClass.equals(Short.class)) {
						Array.setShort(toReturn, i, ((Short)ds.o).shortValue());
					} else if(arrayClass.equals(Integer.class)) {
						Array.setInt(toReturn, i, ((Integer)ds.o).intValue());
					} else if(arrayClass.equals(Long.class)) {
						Array.setLong(toReturn, i, ((Long)ds.o).longValue());
					} else if(arrayClass.equals(Float.class)) {
						Array.setFloat(toReturn, i, ((Float)ds.o).floatValue());
					} else if(arrayClass.equals(Double.class)) {
						Array.setDouble(toReturn, i, ((Double)ds.o).doubleValue());
					}					
				} else {
					Array.set(toReturn, i, ds.o);
				}
				bytesRead += ds.bytes;				
			}
			
			bytesRead++; // magic
		} else if(c == Integer.class || c == int.class) {
			toReturn = new Integer(arrayToInteger(bytes, offset));
			bytesRead += 4;
		}else if(c == Long.class || c == long.class) {
			int bytesInLong = arrayToInteger(bytes, offset);
			byte[] longBytes = new byte[bytesInLong - 4];
			System.arraycopy(bytes, offset + 4, longBytes, 0, bytesInLong - 4);
			toReturn = new Long(Long.valueOf(new String(longBytes), 16));
			bytesRead += bytesInLong;
		} else if(c == Float.class || c == float.class) {
			toReturn = new Float(Float.intBitsToFloat(arrayToInteger(bytes, offset)));
			bytesRead += 4;
		} else if(c == Byte.class || c == byte.class) {
			toReturn = new Byte(bytes[offset]);
			bytesRead += 1;
		}else if(c == Boolean.class || c == boolean.class) {
			if(bytes[offset] == '\0') toReturn = new Boolean(false);
			else toReturn = new Boolean(true);
			bytesRead += 1;
		} else if(c == Short.class || c == short.class) {
			toReturn = new Short(arrayToShort(bytes, offset));
			bytesRead += 2;
		} else if(c == String.class) {
			int start = bytesRead;
			bytesRead += arrayToInteger(bytes, offset);
			byte[] strBytes = new byte[bytesRead - start - 4];
			System.arraycopy(bytes, offset + 4, strBytes, 0, bytesRead - start - 4);
			toReturn = new String(strBytes);
		} else if(c == NetAddress.class) {
			toReturn = new NetAddress(arrayToInteger(bytes, offset));
			bytesRead += 4;
		} else if(c == ArrayList.class) {
			// read the number of objects to deserialize
			int elements = arrayToInteger(bytes, offset);
			bytesRead += 4;
			
			// Instantiate an arrayList
			toReturn = new ArrayList();
			
			// for each element...
			for(int i = 0; i < elements; i++) {
				
				// read the null-terminated class name
				int chars = 0;
				while(bytes[offset + bytesRead] != '\0') {
					bytesRead++;
					chars++;
				}
				byte[] className = new byte[bytesRead - 4];
				System.arraycopy(bytes, offset + bytesRead - chars, className, 0, chars);
				String classString = new String(className);
				Class arrayClass = Class.forName(classString);
				bytesRead++; // for the null terminating character
								
				// read the object itself
				Deserialized ds = deserialize(bytes, offset + bytesRead, arrayClass);
				((ArrayList) toReturn).add(ds.o);
				bytesRead += ds.bytes;
			}
			
		} else if(c == Vector.class) {
			// read the number of objects to deserialize
			int elements = arrayToInteger(bytes, offset);
			bytesRead += 4;
			
			// Instantiate an arrayList
			toReturn = new Vector();
			
			// for each element...
			for(int i = 0; i < elements; i++) {
				
				// read the null-terminated class name
				int chars = 0;
				while(bytes[offset + bytesRead] != '\0') {
					bytesRead++;
					chars++;
				}
				byte[] className = new byte[bytesRead - 4];
				System.arraycopy(bytes, offset + bytesRead - chars, className, 0, chars);
				String classString = new String(className);
				Class arrayClass = Class.forName(classString);
				bytesRead++; // for the null terminating character
								
				// read the object itself
				Deserialized ds = deserialize(bytes, offset + bytesRead, arrayClass);
				((Vector) toReturn).add(ds.o);
				bytesRead += ds.bytes;
			}
			
		} else if(c == LinkedList.class) {
			toReturn = new LinkedList();
			// read the number of objects to deserialize
			int objs = arrayToInteger(bytes, offset);
			bytesRead += 4;
			for(int j = 0; j < objs; j++) {
				// read the object length
				int length = arrayToInteger(bytes, offset + bytesRead);
				byte[] tempBytes = new byte[length];
				System.arraycopy(bytes, offset + bytesRead, tempBytes, 0, length);
				((LinkedList) toReturn).add(deserialize(tempBytes));
				bytesRead += length;
			}
		} else {		
			// Get the byte length of this object (first four bytes)
			bytesRead += arrayToInteger(bytes, offset);
			
			// Copy that many bytes to a new array, and deserialize
			byte[] objBytes = new byte[bytesRead];
			System.arraycopy(bytes, offset - 1, objBytes, 0, bytesRead);
			toReturn = deserialize(objBytes);
		}
		
		return new Pickle().new Deserialized(toReturn, bytesRead);
	}
	
	public static Object deserialize(byte[] bytes) throws Exception {
		
		// read the first byte; see if it's 0 or 1
		if(bytes[0] == '\0') return null;
		
		// Determine the class name specified in the byte array.
		int current = 5;
		while(current < bytes.length) {
			if(bytes[current] == '\0') break;
			current++;
		}
		
		byte[] className = new byte[current - 5];
		System.arraycopy(bytes, 5, className, 0, current - 5);
		String classString = new String(className);
		
		// move past the null character at the end of the class name
		current++;
		
		Class objectToMake = Class.forName(classString);

		// Determine if the named class can be deserialized (if not, throw exception!)
		if(!implementsOurSerializable(objectToMake)) {
			System.err.println("Class " + objectToMake.getName() + " cannot be deserialized!");
			throw new Exception();
		}
		
		// instantiate the specific object type
		Object o = objectToMake.newInstance();
		
		// get a reference to the fieldNames array declared in the object class
		String[] fieldNames = (String[]) o.getClass().getField("fieldNames").get(o);
		
		// Fill in the fields
		for(int i = 0; i < fieldNames.length; i++) {
			Deserialized ds = deserialize(bytes, current, o.getClass().getField(fieldNames[i]).getType());
			Object newVal = ds.o;
			current += ds.bytes;
			o.getClass().getField(fieldNames[i]).set(o, newVal);
		}
		
	// return the new object		
	return o;
  }
	
	//
	// End Functions for serializing complex classes
	//
	
  //////////////////////////////////////////////////
  // Pretty print byte arrays
  //

  public static void printByteArrayNicely(byte[] a)
  {
    HexDumpEncoder hde = new HexDumpEncoder();
    System.out.print(hde.encode(a));
  }
  
  public static void printByteArrayNicely(byte[] a, int offset, int length) 
  {
    byte[] b = new byte[length];
    System.arraycopy(a, offset, b, 0, length);
    printByteArrayNicely(b);
  }
  
  public static void printlnByteArrayNicely(byte[] a, int offset, int length)
  {
    printByteArrayNicely(a, offset, length);
    System.out.println();
  }

  public static void printlnByteArrayNicely(byte[] a)
  {
    printlnByteArrayNicely(a, 0, a.length);
  }

  /**
   * Utility method to stuff an entire
   * enumeration into a vector
   */
  public static Vector Enum2Vector(Enumeration e) 
  {
    Vector v=new Vector();
    while(e.hasMoreElements()) 
    {
      v.add(e.nextElement());
    }
    return v;
  }


  //////////////////////////////////////////////////
  // Helper methods for dealing with byte[]'s
  //

  /** 
   * Handle "unsigned" byte arrays containing numbers larger than 128
   * (bytes are signed, so convert into ints)
   */
  public static int[] byteToIntArray(byte[] data, int offset, int length) 
  {
    int[] temp = new int[length];
    for (int i = 0; i < length; i++) {
      temp[i] = (int)data[i+offset]<0 
        ? 256+(int)data[i+offset] 
        : (int)data[i+offset];
    }
    return temp;
  }

  public static byte[] intToByteArray(int[] data, int offset, int length) 
  {
    byte[] temp = new byte[length];
    for (int i = 0; i < length; i++) 
    {
      if(data[i+offset]<0 || data[i+offset]>255) 
      {
        throw new RuntimeException("number too large for unsigned byte");
      }
      temp[i] = data[i+offset]>127
        ? (byte)(data[i+offset]-256)
        : (byte)data[i+offset];
    }
    return temp;
  }

  public static int[] byteToIntArray(byte[] data) 
  {
    return byteToIntArray(data, 0, data.length);
  }

  public static byte[] intToByteArray(int[] data) 
  {
    return intToByteArray(data, 0, data.length);
  }

  public static byte[] concat(byte[] b1, byte[] b2)
  {
    byte[] b = new byte[b1.length+b2.length];
    System.arraycopy(b1, 0, b, 0, b1.length);
    System.arraycopy(b2, 0, b, b1.length, b2.length);
    return b;
  }

  //////////////////////////////////////////////////
  // unsigned bytes
  //

  public static final void ubyteToArray(int ubyte, byte[] b, int offset)
  {
    b[offset] = ubyte<128 ? (byte)ubyte : (byte)(ubyte-256);
  }

  public static final int arrayToUByte(byte[] b, int offset)
  {
    return b[offset]<0 ? 256+(int)b[offset] : (int)b[offset];
  }

  //////////////////////////////////////////////////
  // unsigned short
  //

  public static final void ushortToArray(int ushort, byte[] b, int offset)
  {
    ubyteToArray((byte)(ushort>>8), b, offset);
    ubyteToArray((byte)ushort, b, offset+1);
  }

  public static final int arrayToUShort(byte[] b, int offset)
  {
    return (arrayToUByte(b, offset)<<8) + arrayToUByte(b, offset+1);
  }

  //////////////////////////////////////////////////
  // unsigned int
  //

  public static final void uintToArray(long uint, byte[] b, int offset)
  {
    ushortToArray((int)(uint>>16), b, offset);
    ushortToArray((int)uint, b, offset+2);
  }

  public static final long arrayToUInt(byte[] b, int offset)
  {
    return (arrayToUShort(b, offset)<<16) + arrayToUShort(b, offset+2);
  }



  /**
   * Integer: size = 4
   */
  public static void integerToArray(int integer, byte[] b, int offset)
  {
    b[offset]   = (byte)integer;
    b[offset+1] = (byte)(integer>>8);
    b[offset+2] = (byte)(integer>>16);
    b[offset+3] = (byte)(integer>>24);
  }

  public static int arrayToInteger(byte[] b, int offset)
  {
    int[] i=byteToIntArray(b, offset, 4);
    return i[0] + (i[1]<<8) + (i[2]<<16) + (i[3]<<24);
  }

  /**
   * Short: size = 2
   */
  public static void shortToArray(short i, byte[] b, int offset)
  {
    b[offset]   = (byte)i;
    b[offset+1] = (byte)(i>>8);
  }

  public static short arrayToShort(byte[] b, int offset)
  {
    int[] i = byteToIntArray(b, offset, 2);
    return (short)(i[0]+(i[1]<<8));
  }

  /**
   * InetAddress: size = 4
   */
  public static void InetAddressToArray(InetAddress inet, byte[] b, int offset)
  {
    System.arraycopy(inet.getAddress(), 0, b, offset, 4);
  }

  public static InetAddress arrayToInetAddress(byte[] addr, int offset)
  {
    int[] i = byteToIntArray(addr, offset, 4);
    String s=i[0]+"."+i[1]+"."+i[2]+"."+i[3];
    try 
    {
      return InetAddress.getByName(s);
    }
    catch(UnknownHostException e)
    {
      throw new RuntimeException("unknown host: "+s);
    }
  }

  /**
   * String: size = variable
   */
  public static int getLength(byte[] b, int offset)
  {
    int len = arrayToInteger(b, offset);
    return Math.max(0, len)+4;
  }

  public static byte[] stringToArray(String s)
  {
    byte[] out = null;
    if(s==null) 
    {
      out = new byte[4];
      integerToArray(-1, out, 0);
    }
    else 
    {
      byte[] sb = s.getBytes();
      out = new byte[sb.length+4];
      integerToArray(sb.length, out, 0);
      System.arraycopy(sb, 0, out, 4, sb.length);
    }
    return out;
  }

  public static String arrayToString(byte[] b, int offset)
  {
    int len = arrayToInteger(b, offset);
    if(len==-1)
    {
      return null;
    }
    else 
    {
      return new String(b, offset+4, len);
    }
  }

  /**
   * Object: size = variable
   */
  public static byte[] objectToArray(Object s)
  {
    try 
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(s);
      oos.close();
      baos.close();
      byte[] sb = baos.toByteArray();
      byte[] out = new byte[sb.length+4];
      integerToArray(sb.length, out, 0);
      System.arraycopy(sb, 0, out, 4, sb.length);
      return out;
    }
    catch(IOException e) 
    {
      e.printStackTrace();
      throw new RuntimeException("unable to serialize packet", e);
    }
  }

  public static Object arrayToObject(byte[] b, int offset)
  {
    try 
    {
      int len = arrayToInteger(b, offset);
      ByteArrayInputStream bais = new ByteArrayInputStream(b, offset+4, len);
      ObjectInputStream ois = new ObjectInputStream(bais);
      return ois.readObject();
    }
    catch(IOException e) 
    {
      e.printStackTrace();
      throw new RuntimeException("unable to deserialize packet (io error)", e);
    }
    catch(ClassNotFoundException e) 
    {
      e.printStackTrace();
      throw new RuntimeException("unable to deserialize packet (class not found)", e);
    }
  }

  public static Object arrayToObject(byte[] b)
  {
    return arrayToObject(b, 0);
  }

  public static byte[] messageBytes(Message m)
  {
    byte[] b = new byte[m.getSize()];
    m.getBytes(b, 0);
    return b;
  }

} // class: Pickle

