package generator;

import icsrv20192.D;
import icsrv20192.P;
import uniandes.gload.core.LoadGenerator;
import uniandes.gload.core.Task;

public class Generator {
	
	private LoadGenerator generator;
	public static int numberOfTasks = 20;
	public static int gapBetweenTasks=20;

	public Generator(){
		
		Task work= createTask();
		
		generator= new LoadGenerator("Client - Server Load Test", numberOfTasks, work, gapBetweenTasks);
		generator.generate(); 
		D.generateSheet(numberOfTasks+"-Escenario", P.numThreads , numberOfTasks);
	}
	private Task createTask(){
		return new Cliente();
	}
	public static void main (String ... args){
		@SuppressWarnings("unused")
		Generator gen= new Generator();
	}
	
	
}
