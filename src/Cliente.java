import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.crypto.tls.SSL3Mac;
import org.omg.CORBA.PUBLIC_MEMBER;

public class Cliente {
	
	private final static int PORT = 1245;
	private final static String HOST = "localhost";
	
	private final static String ALGORITMOS = "ALGORITMOS";
	private final static String PADDING = "AES/ECB/PKCS5Padding";
	
	private enum  ALG
	{
	    AES("AES"),
	    BLOWFISH("BLOWFISH"), 
	    RSA("RSA"),
	    HMACSHA1("HMACSHA1"),
	    HMACSHA256("HMACSHA256"),
	    HMACSHA384("HMACSHA384"), 
	    HMACSHA512("HMACSHA512");
		private final String S;
		private ALG(final String S){
			this.S=S;
		}
		public String getS(){
			return S;
		}
	}
	
	private static String mensajeServidor;
//	private static ServerSocket ss;
	private static Socket cs;
	private static PrintWriter writer;
	private static BufferedReader reader;
	private static PublicKey llavePublicaServidor;
	
	public static void extraerPKCD(String fromServer) throws CertificateException{
		//Se parsea el formato hexadecimal del certificado del servidor a un formato de certificado X509
		byte[] certificadoBytes = DatatypeConverter.parseBase64Binary(fromServer);
		CertificateFactory creador = CertificateFactory.getInstance("X.509");
		InputStream in =  new ByteArrayInputStream(certificadoBytes);
		X509Certificate certificadoServidor = (X509Certificate)creador.generateCertificate(in);
		
		//Se obtiene la llave publica del servidor a partir del certificado
		llavePublicaServidor = certificadoServidor.getPublicKey();
	}
	
	public static byte[] cifrarA (Key llave, String algoritmo, String texto){
		byte[] textoCifrado;
		
		try{
			Cipher cifrador = Cipher.getInstance(algoritmo);
			byte[] textoClaro = texto.getBytes();
			
			cifrador.init(Cipher.ENCRYPT_MODE, llave);
			textoCifrado = cifrador.doFinal(textoClaro);
			
			return textoCifrado;
		}
		catch(Exception e){
			System.out.println("Excepcion: "+e.getMessage());
			return null;
		}
	}
	public static byte[] descifrarS (SecretKey llave, byte[] texto){
		byte[] textoClaro;
		try {
			Cipher cifrador= Cipher.getInstance(PADDING);
			cifrador.init(Cipher.DECRYPT_MODE, llave);
			textoClaro= cifrador.doFinal(texto);

		} catch (Exception e) {
			System.out.println("Exception: "+ e.getMessage());
			return null;
		}
		return textoClaro;
	}
	
	
	
   public static byte[] verficarLongCadena(String cadena){ 
		String res = cadena;
		while(res.length()%4!=0){
			res="0"+res;
			System.out.println(res);
			
		}
		System.out.println("length: "+res.length());
	return DatatypeConverter.parseBase64Binary(res);
}
	
	
   public static byte[] verficarLongCadena16(String cadena){ 
		String res = cadena;
		while(res.length()%16!=0){
			res="0"+res;
			System.out.println(res);
			
		}
		System.out.println("length: "+res.length());
	return DatatypeConverter.parseBase64Binary(res);
}
	public static void main(String[] args) {
		try {
			cs = new Socket(HOST, PORT);
			writer = new PrintWriter(cs.getOutputStream(), true);
			reader = new BufferedReader(new InputStreamReader(cs.getInputStream()));
			
			writer.println("HOLA");
			System.out.println(reader.readLine());
			
			writer.println(ALGORITMOS+":"+ALG.AES.getS()+":"+ALG.RSA.getS()+":"+ALG.HMACSHA256.getS());
			System.out.println(reader.readLine());
			
			String cd = reader.readLine();
			System.out.println(cd);
			extraerPKCD(cd);
			KeyGenerator keygen= KeyGenerator.getInstance("AES");		
			SecretKey ks= keygen.generateKey();	
	
			System.out.println("Baina: "+DatatypeConverter.printBase64Binary(ks.getEncoded()));
			String a = new String(ks.getEncoded());
			byte[] ksCifrada = cifrarA(llavePublicaServidor, ALG.RSA.getS(), a);
			String ksCifradaS= DatatypeConverter.printBase64Binary(ksCifrada);
			
			System.out.println("llave de sesion: "+ DatatypeConverter.printBase64Binary(ks.getEncoded()));
			writer.println(ksCifradaS);
			
			writer.println("reto");
			System.out.println("se envió: reto");
			
			cd = reader.readLine();
			System.out.println("length cd: "+cd.length());
			System.out.println("Reto cifrado: "+cd);
			byte[] retoDesc = descifrarS(ks,verficarLongCadena(cd)); 
			String a1=DatatypeConverter.printBase64Binary(retoDesc);
			System.out.println("reto descifrado: "+a1);
			
//			System.out.println("se recibe cod encriptado: "+cd);
//			
//			byte[] prueba1=verficarLongCadena(cd);
//			System.out.println("se verifica cadena : "+prueba1);
//			
//			byte[] prueba = descifrarS(sk, prueba1);
//			System.out.println("se descifra cadena : "+prueba);
//			
//			String retoD = DatatypeConverter.printBase64Binary(prueba);
//			
//			System.out.println("se descifró reto: "+retoD);
			
		} catch (IOException | CertificateException | NoSuchAlgorithmException e) {
		
			e.printStackTrace();
		} 


	}
}
