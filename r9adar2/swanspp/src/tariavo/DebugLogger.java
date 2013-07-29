package tariavo;

import jist.runtime.JistAPI.DoNotRewrite;


public class DebugLogger implements DoNotRewrite {
	public void info(Object o) {
		System.out.println("INFO\t" + o);
	}
	public void warn(Object arg0) {
		System.out.println("WARN\t" + arg0);
	}
	public void fatal(Object o) {
		System.out.println("FATAL\t" + o);
	}
}
