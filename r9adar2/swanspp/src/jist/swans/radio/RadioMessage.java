package jist.swans.radio;

import jist.swans.Constants;
import jist.swans.misc.Message;

/** 
 *
 * implements a message which piggybacks the channel info on a message
 * 
 * @author Tom Lippmann (tomlippmann_at_users.sourceforge.net)
 * @version 1.0 (2007-23-01)
 * 
 */
public class RadioMessage implements Message
{

	public String getMessageID()
	  {
		  return "not implemented";
	  }
	
  //////////////////////////////////////////////////
  // channel info
  //

   /**
   * channel.
   */
  private int channel;
  
  /**
   * Packet data payload.
   */
  private Message body;

  //////////////////////////////////////////////////
  // initialization
  //

  /**
   * Create a radio packet (=macpacket + channel info)
   * 
   * @param currentChannel frequency channel
   * 
   */
  protected RadioMessage(int currentChannel)
  {
    this.channel = currentChannel;
  }

  //////////////////////////////////////////////////
  // accessors
  //

  /**
   * Return channel
   *
   * @return channel
   */
  public int getChannel()
  {
    return channel;
  }

  /**
   * Return packet data payload.
   *
   * @return packet data payload
   */
  public Message getBody()
  {
    return body;
  }
  
    //////////////////////////////////////////////////
    // initialization
    //

    /**
     * 
     */
    public RadioMessage(int currentChannel, Message body)
    {
      this(currentChannel);
      this.body = body;
    }
  
    //////////////////////////////////////////////////
    // message interface 
    //

    // Message interface
    /** {@inheritDoc} */
    public int getSize()
    {
      int size = body.getSize();
      if(size==Constants.ZERO_WIRE_SIZE)
      {
        return Constants.ZERO_WIRE_SIZE;
      }
      return size;
    }

    // Message interface
    /** {@inheritDoc} */
    public void getBytes(byte[] msg, int offset)
    {
      throw new RuntimeException("todo: not implemented");
    }

  

} // class: RadioMessage

