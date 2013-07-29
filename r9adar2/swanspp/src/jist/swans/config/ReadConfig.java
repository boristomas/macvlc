package jist.swans.config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;


public class ReadConfig
{
	public static LinkedList listOfSimulations;
	
	public static void readSimulationList(String args)
	{
//		 2006-03-24, 16.10. Making some bigger changes so that we can run several simulations in a row without
		// human interference =)
		listOfSimulations = new LinkedList();
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(args));
			String line = "";
			while((line = br.readLine()) != null) 
			{
				int i = line.indexOf("#");
				if (i != -1)
					line = line.substring(0, i);
				line = line.trim();
				if (line.length() > 0)
				{
					listOfSimulations.addLast(line);
				}
			}
		}
		catch (FileNotFoundException ex) 
		{
			ex.printStackTrace();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		Configuration.listOfSimulations = listOfSimulations;
	}
	
	public static void readSpecialSettings(String path)
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(path));
			String line = "";
			boolean asterisk = false;
			String overrideMovementString = "";
			while((line = br.readLine()) != null) 
			{
				int i = line.indexOf("#");
				if (i != -1)
					line = line.substring(0, i);
				line = line.trim();
				if (line.length() > 0)
				{
					if (line.startsWith("CarMovements="))
					{
						line = line.substring(13, line.length());
						// If the line is marked with an * that means this movements should affect all nodes.
						if (line.substring(0, line.indexOf(":")).equals("*") && !asterisk)
						{
							// Found an entry that is applied to all cars.
							asterisk = true;
							overrideMovementString = line.substring(line.indexOf(":")+1, line.length());
							System.out.println("ASTERISK!");
							Configuration.carMovements.put(new Integer(-1), new Movement(1, overrideMovementString));
						}
						// If not it is a normal line
						else
						{
							String carId = line.substring(0, line.indexOf(":"));
							// Has there been an asterisk before?
							// 2006-04-03: Doesnt matter anymore, changed it.
							//if (!asterisk)
							{
								//System.out.println("No, add as usual");
								// No, add as usual
								String segments = line.substring(line.indexOf(":")+1, line.length());
								Configuration.carMovements.put(new Integer(carId), new Movement(1, segments));
							}
							/*else if (asterisk)
							{
								System.out.println("Yes, just add the overrideMovementString: "+overrideMovementString);
								// Yes, just add the overrideMovementString
								Configuration.carMovements.put(new Integer(carId), new Movement(1, overrideMovementString));
							}
							*/
						}
						
					}
					if (line.startsWith("SpecialEvent="))
					{
						line = line.substring(13, line.length());
						String[] eventData = line.split(":");
						Integer carId = new Integer(eventData[0]);
						int type = new Integer(eventData[1]).intValue();
						long time = new Long(eventData[2]).longValue();
						LinkedList ll;
						if (!Configuration.carSpecialEvents.containsKey(carId))
						{
							ll = new LinkedList();
							Configuration.carSpecialEvents.put(carId, ll);
						}
						else
							ll = (LinkedList) Configuration.carSpecialEvents.get(carId);
						
						Event e = new Event(time, type);
						for (int k = 2; k < eventData.length; k++)
						{
							if (type == 1)
							{
								// Stop vehicle
								e.metersPerSecond = 0;
								e.acceleration = 0;
							}
							else if (type == 2)
							{
								// Change speed
								e.metersPerSecond = new Integer(eventData[3]).intValue();
							}
						}
						ll.addLast(e);
					}	
				}
			}
		}
		catch (FileNotFoundException ex) 
		{
			System.out.println("FileNotFound got damit!");
			ex.printStackTrace();
		}
		catch (IOException ex)
		{
			System.out.println("IOException got damit!");
			ex.printStackTrace();
		}
		Configuration.listOfSimulations = listOfSimulations;
    }
}