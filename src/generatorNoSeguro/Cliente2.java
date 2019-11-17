package generatorNoSeguro;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import icsrv20192.D;
import uniandes.gload.core.Task;

public class Cliente2 extends Task{

	private final static int PORT = 54321;
	private final static String HOST = "localhost";

	private final static String ALGORITMOS = "ALGORITMOS";

	private enum ALG {
		AES("AES"), BLOWFISH("BLOWFISH"), RSA("RSA"), HMACSHA1("HMACSHA1"), HMACSHA256("HMACSHA256"), HMACSHA384(
				"HMACSHA384"), HMACSHA512("HMACSHA512");
		private final String S;

		private ALG(final String S) {
			this.S = S;
		}

		public String getS() {
			return S;
		}
	}
   public Cliente2(){
	   
   }
	
	// private static ServerSocket ss;
	private static Socket cs;
	
	@Override
	public  void fail() {
		System.out.println("Hubo un fallo en el proceso");
		D.transaccionesPerdidas++;
	}

	@Override
	public void success() {
		System.out.println("Se realizó correctamente");
		
	}

	@Override
	public void execute() {
		String cc ="" +Math.random()*10000;
		String clave = ""+ Math.random()*100000;
		try {
			boolean ok = true;
			cs = new Socket(HOST, PORT);
			PrintWriter writer = new PrintWriter(cs.getOutputStream(), true);
			BufferedReader reader = new BufferedReader(new InputStreamReader(cs.getInputStream()));

			writer.println("HOLA");
			System.out.println(reader.readLine());

			writer.println(ALGORITMOS + ":" + ALG.AES.getS() + ":" + ALG.RSA.getS() + ":" + ALG.HMACSHA256.getS());
			System.out.println(reader.readLine());

			String cd = reader.readLine();
			System.out.println("Certificado del servidor: "+cd);
			
			KeyGenerator keygen = KeyGenerator.getInstance("AES");
			SecretKey ks = keygen.generateKey();

			System.out.println("Llave simetrica de sesion: " + DatatypeConverter.printBase64Binary(ks.getEncoded()));
			String ksCifradaS = new String(ks.getEncoded());
			writer.println(ksCifradaS);

			String reto = "reto";
			writer.println(reto);
			System.out.println("se envió: " + reto);

			cd = reader.readLine();
			System.out.println("Reto recibido: " + cd);
			try {
				if (!cd.equals(reto)) {
					writer.println("ERROR");
					ok = false;
					fail();
				} else {
					writer.println("OK");
				}
			} catch (Exception e) {
				writer.println("ERROR");
				ok = false;
				fail();
			}
			if (ok) {

				writer.println(cc);
				System.out.println("se envió cc: " + cc);

				writer.println(clave);
				System.out.println("se envió clave: " + clave);

				String valor = reader.readLine();
				
				System.out.println("Valor del ahorro de la cuenta: " + valor);

				String hvalor = reader.readLine();
				System.out.println("hvalor recibido: " + hvalor);
			
				int hashCode = valor.hashCode();
				System.out.println("hvalor calculado: " + hashCode);
				if (!(hashCode==Integer.parseInt(hvalor))) {
					writer.println("ERROR");
					ok = false;
					fail();
				}
				else{
					writer.println("OK");
					success();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
			
	}
}