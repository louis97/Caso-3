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

public class Cliente {

	private final static int PORT = 1245;
	private final static String HOST = "localhost";

	private final static String ALGORITMOS = "ALGORITMOS";
	private final static String PADDING = "AES/ECB/PKCS5Padding";

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

	// private static ServerSocket ss;
	private static Socket cs;
	private static PrintWriter writer;
	private static BufferedReader reader;
	private static PublicKey llavePublicaServidor;

	public static void extraerPKCD(String fromServer) throws CertificateException {
		// Se parsea el formato hexadecimal del certificado del servidor a un
		// formato de certificado X509
		byte[] certificadoBytes = DatatypeConverter.parseBase64Binary(fromServer);
		
		CertificateFactory creador = CertificateFactory.getInstance("X.509");
		InputStream in = new ByteArrayInputStream(certificadoBytes);
		X509Certificate certificadoServidor = (X509Certificate) creador.generateCertificate(in);

		// Se obtiene la llave publica del servidor a partir del certificado
		llavePublicaServidor = certificadoServidor.getPublicKey();
	}

	public static byte[] cifrarA(Key llave, String algoritmo, String texto) {
		byte[] textoCifrado;

		try {
			Cipher cifrador = Cipher.getInstance(algoritmo);
			byte[] textoClaro = texto.getBytes();

			cifrador.init(Cipher.ENCRYPT_MODE, llave);
			textoCifrado = cifrador.doFinal(textoClaro);

			return textoCifrado;
		} catch (Exception e) {
			System.out.println("Excepcion: " + e.getMessage());
			return null;
		}
	}

	public static byte[] cifrarS(SecretKey llave, String algoritmo, String texto) {
		byte[] textoCifrado;

		try {
			Cipher cifrador = Cipher.getInstance(PADDING);
			byte[] textoClaro = texto.getBytes();

			cifrador.init(Cipher.ENCRYPT_MODE, llave);
			textoCifrado = cifrador.doFinal(textoClaro);

			return textoCifrado;
		} catch (Exception e) {
			System.out.println("Excepcion: " + e.getMessage());
			return null;
		}
	}

	public static byte[] descifrarS(SecretKey llave, byte[] texto) {
		byte[] textoClaro;
		try {
			Cipher cifrador = Cipher.getInstance(PADDING);
			cifrador.init(Cipher.DECRYPT_MODE, llave);
			textoClaro = cifrador.doFinal(texto);

		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
			return null;
		}
		return textoClaro;
	}

	public static byte[] descifrarA(Key llave, String algoritmo, byte[] texto) {
		byte[] textoClaro;

		try {

			Cipher cifrador = Cipher.getInstance(algoritmo);
			cifrador.init(Cipher.DECRYPT_MODE, llave);
			textoClaro = cifrador.doFinal(texto);

		} catch (Exception e) {

			System.out.println("Excepcion: " + e.getMessage());
			return null;

		}

		return textoClaro;
	}

	public static byte[] verficarLongCadena(String cadena) {
		String res = cadena;
		while (res.length() % 4 != 0) {
			res = "0" + res;

		}
		return DatatypeConverter.parseBase64Binary(res);
	}

	public static byte[] verficarLongCadena16(String cadena) {
		String res = cadena;
		while (res.length() % 16 != 0) {
			System.out.println(res);
		}
		return DatatypeConverter.parseBase64Binary(res);
	}

	public static void main(String[] args) {
		
		try {
			boolean ok = true;
			cs = new Socket(HOST, PORT);
			writer = new PrintWriter(cs.getOutputStream(), true);
			reader = new BufferedReader(new InputStreamReader(cs.getInputStream()));

			writer.println("HOLA");
			System.out.println(reader.readLine());

			writer.println(ALGORITMOS + ":" + ALG.AES.getS() + ":" + ALG.RSA.getS() + ":" + ALG.HMACSHA256.getS());
			System.out.println(reader.readLine());

			String cd = reader.readLine();
			System.out.println("Certificado del servidor: "+cd);
			extraerPKCD(cd);
			KeyGenerator keygen = KeyGenerator.getInstance("AES");
			SecretKey ks = keygen.generateKey();

			System.out.println("Llave simetrica de sesion: " + DatatypeConverter.printBase64Binary(ks.getEncoded()));
			String a = new String(ks.getEncoded());
			byte[] ksCifrada = cifrarA(llavePublicaServidor, ALG.RSA.getS(), a);
			String ksCifradaS = DatatypeConverter.printBase64Binary(ksCifrada);
			writer.println(ksCifradaS);

			String reto = "reto";
			writer.println(reto);
			System.out.println("se envió: " + reto);

			cd = reader.readLine();
			System.out.println("Reto cifrado: " + cd);
			try {
				byte[] retoDesc = descifrarS(ks, verficarLongCadena(cd));
				String a1 = DatatypeConverter.printBase64Binary(retoDesc);
				System.out.println("reto descifrado: " + a1);
				if (!a1.equals(reto)) {
					writer.println("ERROR");
					ok = false;
				} else {
					writer.println("OK");
				}
			} catch (Exception e) {
				writer.println("ERROR");
				ok = false;
			}
			if (ok) {
				Scanner sc = new Scanner(System.in);
				System.out.println("Ingrese su cedula: ");
				String cc = sc.nextLine();
				byte[] ccCifrado = cifrarS(ks, "", cc);
				String ccCifradoS = DatatypeConverter.printBase64Binary(ccCifrado);
				writer.println(ccCifradoS);

				System.out.println("Ingrese su clave: ");
				String clave = sc.nextLine();
				byte[] claveCifrado = cifrarS(ks, "", clave);
				String claveCifradoS = DatatypeConverter.printBase64Binary(claveCifrado);
				writer.println(claveCifradoS);

				String cifradoValor = reader.readLine();
				byte[] descifradoValor = descifrarS(ks, verficarLongCadena(cifradoValor));
				System.out.println("Valor del ahorro de la cuenta: " + DatatypeConverter.printBase64Binary(descifradoValor));

				String hmacC = reader.readLine();
				byte[] hmacB = descifrarA(llavePublicaServidor, ALG.RSA.getS(), verficarLongCadena(hmacC));
				String hmacBS = DatatypeConverter.printBase64Binary(hmacB);

				HMac hmac = new HMac(new SHA256Digest());;
				hmac.init(new KeyParameter(ks.getEncoded()));
				byte[] hashBytes = new byte[hmac.getMacSize()];
				hmac.update(descifradoValor, 0, descifradoValor.length);
				hmac.doFinal(hashBytes, 0);

				String hmacR = DatatypeConverter.printBase64Binary(hashBytes);
				System.out.println("hmac calculado: " + hmacR);

				if (!(hmacR.equals(hmacBS))) {
					writer.println("ERROR");
					ok = false;
				}
				else{
					writer.println("OK");
				}
			}

		} catch (IOException | CertificateException | NoSuchAlgorithmException e) {

			e.printStackTrace();
		}

	}
}
