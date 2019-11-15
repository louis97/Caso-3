package generatorNoSeguro;

import uniandes.gload.core.LoadGenerator;
import uniandes.gload.core.Task;
import uniandes.gload.examples.clientserver.generator.ClientServerTask;

public class Generator2 {
	
	private LoadGenerator generator;

	public Generator2(){
		
		Task work= createTask();
		int numberOfTasks = 100;
		int gapBetweenTasks=1000;
		generator= new LoadGenerator("Client - Server Load Test", numberOfTasks, work, gapBetweenTasks);
		generator.generate();
	}
	private Task createTask(){
		return new Cliente2();
	}
	public static void main (String ... args){
		@SuppressWarnings("unused")
		Generator2 gen= new Generator2();
	}
}
