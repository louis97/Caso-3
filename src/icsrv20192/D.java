package icsrv20192;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.bind.DatatypeConverter;

import generator.Generator;


public class D implements Runnable{

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
	public static double transaccionesPerdidas=0;
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
		file2=pFile2;
	}

	public D (Socket csP, int idP) {
		sc = csP;
		dlg = new String("delegado " + idP + ": ");
		try {
			mybyte = new byte[520]; 
			mybyte = certSer.getEncoded();
		} catch (Exception e) {
			System.out.println("Error creando encoded del certificado para el thread" + dlg);
			transaccionesPerdidas++;
			e.printStackTrace();
		}
	}

	private boolean validoAlgHMAC(String nombre) {
		return ((nombre.equals(S.HMACMD5) || 
				nombre.equals(S.HMACSHA1) ||
				nombre.equals(S.HMACSHA256) ||
				nombre.equals(S.HMACSHA384) ||
				nombre.equals(S.HMACSHA512)
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
				transaccionesPerdidas++;
				sc.close();
				throw new Exception(dlg + ERROR + REC + linea +"-terminando.");
			}

			String[] algoritmos = linea.split(SEPARADOR);
			if (!algoritmos[1].equals(S.DES) && !algoritmos[1].equals(S.AES) &&
					!algoritmos[1].equals(S.BLOWFISH) && !algoritmos[1].equals(S.RC4)){
				ac.println(ERROR);
				transaccionesPerdidas++;
				sc.close();
				throw new Exception(dlg + ERROR + "Alg.Simetrico" + REC + algoritmos + "-terminando.");
			}
			if (!algoritmos[2].equals(S.RSA) ) {
				ac.println(ERROR);
				sc.close();
				transaccionesPerdidas++;
				throw new Exception(dlg + ERROR + "Alg.Asimetrico." + REC + algoritmos + "-terminando.");
			}
			if (!validoAlgHMAC(algoritmos[3])) {
				ac.println(ERROR);
				transaccionesPerdidas++;
				sc.close();
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
			System.out.println("Llave leida por el servidor enviada por el cliente: "+linea);
			long cm1= System.currentTimeMillis();
			byte[] llaveSimetrica = S.ad(
					toByteArray(linea), 
					keyPairServidor.getPrivate(), algoritmos[2] );
			SecretKey simetrica = new SecretKeySpec(llaveSimetrica, 0, llaveSimetrica.length, algoritmos[1]);
			cadenas[3] = dlg + "recibio y creo llave simetrica. continuando.";
			System.out.println(cadenas[3]);

			/***** Fase 5:  *****/
			cadenas[4]="";
			linea = dc.readLine();
			System.out.println(dlg + "Recibio reto del cliente:-" + linea + "-");
			byte[] retoByte = toByteArray(linea);
			String re = new String(retoByte);
			System.out.println(re);
			byte [ ] ciphertext1 = S.se(retoByte, simetrica, algoritmos[1]);
			ac.println(toHexString(ciphertext1));
			System.out.println(dlg + "envio reto cifrado "+ ciphertext1+" con llave simetrica al cliente. continuado.");

			linea = dc.readLine();
			if ((linea.equals(OK))) {
				cadenas[4] = dlg + "recibio confirmacion del cliente:"+ linea +"-continuado.";
				System.out.println(cadenas[4]);
			} else if(linea.equals(ERROR)){
				sc.close();
				transaccionesPerdidas++;
				throw new Exception(dlg + ERROR + " en confirmacion de llave simetrica." + REC + "-terminando.");
			}

			/***** Fase 6:  *****/
			linea = dc.readLine();				
			byte[] ccByte = S.sd(
					toByteArray(linea), simetrica, algoritmos[1]);
			String cc = toHexString(ccByte);
			System.out.println(dlg + "recibio cc y descifro:-" + cc + "-continuado.");

			linea = dc.readLine();				
			byte[] claveByte = S.sd(
					toByteArray(linea), simetrica, algoritmos[1]);
			String clave = toHexString(claveByte);
			System.out.println(dlg + "recibio clave y descifro:-" + clave + "-continuado.");
			cadenas[5] = dlg + "recibio cc y clave - continuando";

			Random rand = new Random(); 
			int valor = rand.nextInt(1000000);
			String strvalor = valor+"";
			while (strvalor.length()%4!=0) strvalor += 0;
			byte[] valorByte = toByteArray(strvalor);
			byte [ ] ciphertext2 = S.se(valorByte, simetrica, algoritmos[1]);
			ac.println(toHexString(ciphertext2));
			cadenas[6] = dlg + "envio valor "+strvalor+" cifrado con llave simetrica al cliente. continuado.";
			System.out.println(cadenas[6]);

			byte [] hmac = S.hdg(valorByte, simetrica, algoritmos[3]);
			byte[] recibo = S.ae(hmac, keyPairServidor.getPrivate(), algoritmos[2]);
			ac.println(toHexString(recibo));
			System.out.println(dlg + "envio hmac cifrado con llave privada del servidor. continuado.");
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
				escribirMensaje("Tiempo de respuesta de una transacci�n en milis: "+total);
				Double y = getSystemCpuLoad();
				escribirMensaje("Porcentage del CPU usado: "+ y);
				escribirMensaje("Porcentaje de error: "+(transaccionesPerdidas/Generator.numberOfTasks)*100 +"%");
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
		//return Base64.getEncoder().encodeToString(array);
	}

	public static byte[] toByteArray(String s) {
		return DatatypeConverter.parseBase64Binary(s);
		//return Base64.getDecoder().decode(s); 
	}

}
