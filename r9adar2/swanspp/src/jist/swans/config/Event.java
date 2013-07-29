package jist.swans.config;

public class Event {
	
	public long time;
	public int type;
	public int metersPerSecond;
	public int acceleration;
	public String instructions;
	
	public Event(long time, int type)
	{
		this.time = time;
		this.type = type;
		//this.instructions = instructions;
	}
}
