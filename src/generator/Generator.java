package generator;

import uniandes.gload.core.LoadGenerator;
import uniandes.gload.core.Task;
import uniandes.gload.examples.clientserver.generator.ClientServerTask;

public class Generator {
	public static int numberOfTasks = 2;
	public static int gapBetweenTasks= 20;
	private LoadGenerator generator;

	public Generator(){
		
		Task work= createTask();
		
		generator= new LoadGenerator("Client - Server Load Test", numberOfTasks, work, gapBetweenTasks);
		generator.generate();
	}
	private Task createTask(){
		return new Cliente();
	}
	public static void main (String ... args){
		@SuppressWarnings("unused")
		Generator gen= new Generator();
	}
}
