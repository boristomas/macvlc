package tariavo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import jist.runtime.JistAPI;
import jist.runtime.JistAPI.Proxiable;

public class Util {
	public final static DelayInterface delay = (DelayInterface) JistAPI.proxy(
			new Delay(), DelayInterface.class); 
	public static interface DelayInterface extends Proxiable {
		void delay(int i);
	}
	public static class Delay implements DelayInterface {
		public void delay(int i) {
			List list = new ArrayList();
			boolean b = false;
			for(int j = 0; j < i; j++) {
				b^=true;
				list.add(new Double(Math.random()));
			}
			if(b) list.clear();
		}
	}
	
	public static void isEntity(Object ob) {
		if(!JistAPI.isEntity(ob)) throw new IllegalArgumentException
				("expected entity");
	}
	
	public static void writeParams(String desc, int number_nodes, int number_channels, 
			int number_send, double epoch, double appPeriod, int bitrate) {
		File file = new File(number_nodes + "n_" + number_channels + "ch_"
				+ number_send + "s_" + epoch + "ep_" + bitrate + "br_");
		file.mkdir();
		
		try {
			PrintWriter writer = new PrintWriter(new FileWriter(file + 
					File.separator + "params.txt"));
			writer.println("Description: ");
			writer.println(desc);
			writer.println();
			writer.println("number_nodes = " + number_nodes);
			writer.println("number_channels = " + number_channels);
			writer.println("number_send = " + number_send);
			writer.println("epoch =" + epoch);
			writer.println("appPeriod =" + appPeriod);
			writer.println("bitrate = " +  bitrate);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
