package jist.swans.config;

import java.util.LinkedList;

public class Movement {
	
	public long time;
	public int type;
	public String instructions;
	public LinkedList list;
	
	public Movement(int type, String instructions)
	{
		list = new LinkedList();
		this.type = type;
		this.instructions = instructions;
		parseInstructions();
	}
	
	private void parseInstructions()
	{
		if (instructions.length() > 0)
		{
			if (type == 1)
			{
				// Movements
				String[] temp = instructions.split(",");
				for (int i = 0; i < temp.length; i++)
				{
					list.addLast(new Integer(temp[i]));
				}
			}
		}
	}

}
