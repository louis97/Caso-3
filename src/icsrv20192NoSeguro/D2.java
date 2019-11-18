package icsrv20192NoSeguro;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.bind.DatatypeConverter;

import generator.Generator;
import generatorNoSeguro.Generator2;


public class D2 implements Runnable{

	public static final String OK = "OK";
	public static final String ALGORITMOS = "ALGORITMOS";
	public static final String CERTSRV = "CERTSRV";
	public static final String CERCLNT = "CERCLNT";
	public static final String SEPARADOR = ":";
	public static final String HOLA = "HOLA";
	public static final String INICIO = "INICIO";
	public static final String ERROR = "ERROR";
	public static final String REC = "recibio-";
	public static final int numCadenas = 8;
	public static double transaccionesPerdidas = 0;
	// Atributos
	private Socket sc = null;
	private String dlg;
	private byte[] mybyte;
	private static File file;
	private static File file2;
	private static X509Certificate certSer;
	private static KeyPair keyPairServidor;
	
	public static void init(X509Certificate pCertSer, KeyPair pKeyPairServidor, File pFile, File pFile2) {
		certSer = pCertSer;
		keyPairServidor = pKeyPairServidor;
		file = pFile;
		file2 = pFile2;
	}
	
	public D2 (Socket csP, int idP) {
		sc = csP;
		dlg = new String("delegado " + idP + ": ");
		try {
		mybyte = new byte[520]; 
		mybyte = certSer.getEncoded();
		} catch (Exception e) {
			System.out.println("Error creando encoded del certificado para el thread" + dlg);
			e.printStackTrace();
		}
	}
	
	private boolean validoAlgHMAC(String nombre) {
		return ((nombre.equals(S2.HMACMD5) || 
			 nombre.equals(S2.HMACSHA1) ||
			 nombre.equals(S2.HMACSHA256) ||
			 nombre.equals(S2.HMACSHA384) ||
			 nombre.equals(S2.HMACSHA512)
			 ));
	}
	
	/*
	 * Generacion del archivo log. 
	 * Nota: 
	 * - Debe conservar el metodo como está. 
	 * - Es el único metodo permitido para escribir en el log.
	 */
	private void escribirMensaje(String pCadena) {
		
		try {
			FileWriter fw = new FileWriter(file,true);
			fw.write(pCadena + "\n");
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
private void escribirMensaje2(String pCadena) {
		
		try {
			FileWriter fw = new FileWriter(file2,true);
			fw.write(pCadena + "\n");
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void run() {
		String[] cadenas;
		cadenas = new String[numCadenas];
		
		String linea;
	    System.out.println(dlg + "Empezando atencion.");
	        try {

				PrintWriter ac = new PrintWriter(sc.getOutputStream() , true);
				BufferedReader dc = new BufferedReader(new InputStreamReader(sc.getInputStream()));

				/***** Fase 1:  *****/
				linea = dc.readLine();
				cadenas[0] = "Fase1: ";
				if (!linea.equals(HOLA)) {
					ac.println(ERROR);
					transaccionesPerdidas++;
				    sc.close();
					throw new Exception(dlg + ERROR + REC + linea +"-terminando.");
				} else {
					ac.println(OK);
					cadenas[0] = dlg + REC + linea + "-continuando.";
					System.out.println(cadenas[0]);
					
					
				}
				
				/***** Fase 2:  *****/
				linea = dc.readLine();
				cadenas[1] = "Fase2: ";
				if (!(linea.contains(SEPARADOR) && linea.split(SEPARADOR)[0].equals(ALGORITMOS))) {
					ac.println(ERROR);
					sc.close();
					transaccionesPerdidas++;
					throw new Exception(dlg + ERROR + REC + linea +"-terminando.");
				}
				
				String[] algoritmos = linea.split(SEPARADOR);
				if (!algoritmos[1].equals(S2.DES) && !algoritmos[1].equals(S2.AES) &&
					!algoritmos[1].equals(S2.BLOWFISH) && !algoritmos[1].equals(S2.RC4)){
					ac.println(ERROR);
					sc.close();
					transaccionesPerdidas++;
					throw new Exception(dlg + ERROR + "Alg.Simetrico" + REC + algoritmos + "-terminando.");
				}
				if (!algoritmos[2].equals(S2.RSA) ) {
					ac.println(ERROR);
					sc.close();
					transaccionesPerdidas++;
					throw new Exception(dlg + ERROR + "Alg.Asimetrico." + REC + algoritmos + "-terminando.");
				}
				if (!validoAlgHMAC(algoritmos[3])) {
					ac.println(ERROR);
					sc.close();
					transaccionesPerdidas++;
					throw new Exception(dlg + ERROR + "AlgHash." + REC + algoritmos + "-terminando.");
				}
				cadenas[1] = dlg + REC + linea + "-continuando.";
				System.out.println(cadenas[1]);
				ac.println(OK);
				
				/***** Fase 3:  *****/
				String testCert = toHexString(mybyte);
				ac.println(testCert);
				cadenas[2] = dlg + "envio certificado del servidor. continuando.";
				System.out.println(cadenas[2] + testCert);				

				/***** Fase 4: *****/
				cadenas[3] = "";
				linea = dc.readLine();
				long cm1= System.currentTimeMillis();
				
				//SecretKey simetrica = new SecretKeySpec(llaveSimetrica, 0, llaveSimetrica.length, algoritmos[1]);
				cadenas[3] = dlg + "recibio llave simetrica ["+"] . continuando.";
				System.out.println(cadenas[3]);
				
				/***** Fase 5:  *****/
				cadenas[4]="";
				linea = dc.readLine();
				System.out.println(dlg + "Recibio reto del cliente:-" + linea + "-");
				//byte[] retoByte = toByteArray(linea);
				//byte [ ] ciphertext1 = S2.se(retoByte, simetrica, algoritmos[1]);
				ac.println(linea);  //envia reto
				System.out.println(dlg + "envio reto al cliente. continuado.");

				linea = dc.readLine();
				if ((linea.equals(OK))) {
					cadenas[4] = dlg + "recibio confirmacion del cliente:"+ linea +"-continuado.";
					System.out.println(cadenas[4]);
				} else {
					sc.close();
					transaccionesPerdidas++;
					throw new Exception(dlg + ERROR + "en confirmacion de llave simetrica." + REC + "-terminando.");
				}
				
				/***** Fase 6:  *****/
				linea = dc.readLine();				
				//byte[] ccByte = toByteArray(linea);
				String cc = linea;
				System.out.println(dlg + "recibio cc :-" + cc + "-continuado.");
				
				linea = dc.readLine();				
				//byte[] claveByte = toByteArray(linea);
				String clave = linea;
				System.out.println(dlg + "recibio clave:-" + clave + "-continuado.");
				cadenas[5] = dlg + "recibio cc y clave - continuando";
				
				Random rand = new Random(); 
				int valor = rand.nextInt(1000000);
				String strvalor = valor+"";
				while (strvalor.length()%4!=0) strvalor += 0;
				//byte[] valorByte = toByteArray(strvalor);
				//byte [ ] ciphertext2 = S2.se(valorByte, simetrica, algoritmos[1]);
				ac.println(strvalor);
				cadenas[6] = dlg + "envio valor "+strvalor+" al cliente. continuado.";
				System.out.println(cadenas[6]);
		        
//				byte [] hmac = S2.hdg(valorByte, simetrica, algoritmos[3]);
//				byte[] recibo = S2.ae(hmac, keyPairServidor.getPrivate(), algoritmos[2]);
				ac.println(strvalor.hashCode());
				System.out.println(dlg + "envio hash(valor). continuado.");
				long cm2= System.currentTimeMillis();
				long total=cm2-cm1;
				
				cadenas[7] = "";
				linea = dc.readLine();	
				if (linea.equals(OK)) {
					cadenas[7] = dlg + "Terminando exitosamente." + linea;
					System.out.println(cadenas[7]);
				} else {
					cadenas[7] = dlg + "Terminando con error" + linea;
			        System.out.println(cadenas[7]);
			        transaccionesPerdidas++;
				}
			     
		        sc.close();
		        
		        
		        synchronized (file) {
		        	for (int i=0;i<numCadenas;i++) {
					    escribirMensaje(cadenas[i]); 
					   
				    }
		        	Double y = getSystemCpuLoad();
				    escribirMensaje("Tiempo de respuesta de una transacci�n en milis: "+total);
				    escribirMensaje("Porcentage del CPU usado: "+y);
				    escribirMensaje("Porcentaje de error: "+ (transaccionesPerdidas/Generator2.numberOfTasks)*100 + " %");
				    escribirMensaje2(total+";"+ y +";"+(transaccionesPerdidas/Generator.numberOfTasks)*100 +" %");
				}
		        
	        } catch (Exception e) {
	          e.printStackTrace();
	        }
	        
	}
	
	public double getSystemCpuLoad() throws Exception {
		 MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		 ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
		 AttributeList list = mbs.getAttributes(name, new String[]{ "SystemCpuLoad" });
		 if (list.isEmpty()) return Double.NaN;
		 Attribute att = (Attribute)list.get(0);
		 Double value = (Double)att.getValue();
		 // usually takes a couple of seconds before we get real values
		 if (value == -1.0) return Double.NaN;
		 // returns a percentage value with 1 decimal point precision
		 return ((int)(value * 1000) / 10.0);
		 }
	
	public static String toHexString(byte[] array) {
	    return DatatypeConverter.printBase64Binary(array);
	}

	public static byte[] toByteArray(String s) {
	    return DatatypeConverter.parseBase64Binary(s);
	}
	
}