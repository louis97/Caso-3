

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;


import Cifrado.Asimetrico;
import Cifrado.Simetrico;
import ManejoCertificados.BouncyCastle;

public class ProtocoloClienteConSeguridad {

	public static void procesar(BufferedReader pIn, PrintWriter pOut) 
			throws Exception{

		//Cadenas de datos enviados y recibidos del servidor
		String fromServer;
		String fromUser;

		//Algoritmos de cirado usados para la comunicacion
		String algoritmoSimetrico = "";
		String algoritmoAsimetrico = "";
		String algoritmoHMAC = "";

		//LLaves usadas en la comunicacion
		PublicKey llavePublicaServidor = null;
		KeyPair keyPairAsimetrico = null;	
		//PublicKey llavePublica = null; NO SE USA EN EL METODO
		PrivateKey llavePrivada = null;
		SecretKey llaveSimetrica = null;

		int estado = 0;
		while(estado < 5){

			switch(estado){
			case 0: {			

				//El usuario envia la cadena HOLA para iniciar la comunicacion con el servidor
				fromUser = "HOLA";

				System.out.println("El usuario escribio: " + fromUser);

				//envía por la red
				pOut.println(fromUser);
				estado++;

				//lee lo que llega por la red
				//si lo que llega del servidor no es null
				//observe la asignación luego de la condición

				if((fromServer = pIn.readLine()) != null){
					System.out.println("Respuesta del servidor: " + fromServer);	
				}
				System.out.println();
				break;
			}

			case 1:{

				//Se especifican los algoritmos usados en la comunicacion
				//fromUser = "ALGORITMOS:Blowfish:RSA:HMACSHA512";
				fromUser = "ALGORITMOS:AES:RSA:HMACSHA512";
				
				String[] algoritmos = fromUser.split(":");
				algoritmoSimetrico = algoritmos[1];
				algoritmoAsimetrico = algoritmos[2];
				algoritmoHMAC = algoritmos[3];

				System.out.println("El usuario escribio: " + fromUser);

				//envía por la red
				pOut.println(fromUser);
				estado++;


				//lee lo que llega por la red
				//si lo que llega del servidor no es null
				//observe la asignación luego de la condición

				if((fromServer = pIn.readLine()) != null){
					System.out.println("Respuesta del servidor: " + fromServer);	
				}
				System.out.println();
				break;
			}

			case 2:{
				
				//Se crea el generador de llaves asimetricas
				KeyPairGenerator generator = KeyPairGenerator.getInstance(algoritmoAsimetrico);
				//Se inicializa con un tamanio de de llave de 1024 bits
				generator.initialize(1024);
				//Se crean las llaves publica y privada del cliente
				keyPairAsimetrico = generator.generateKeyPair();
				//llavePublica = keyPairAsimetrico.getPublic(); NO SE USA
				llavePrivada = keyPairAsimetrico.getPrivate();

				//Se genera el certificado con el estandar X509
				java.security.cert.X509Certificate certificado = BouncyCastle.generateSelfSignedX509Certificate(keyPairAsimetrico);	
				byte[] certificadoEnBytes = certificado.getEncoded();
				//Se obtiene el certificado en formato hexadecimal
				String certificadoEnString = DatatypeConverter.printHexBinary(certificadoEnBytes);

				System.out.println("Envio Certificado: " + certificadoEnString);
				
				//Se envia el certificado al servidor
				pOut.println(certificadoEnString);
				

				if((fromServer = pIn.readLine()) != null){
					//Se recibe el certificado del servidor en formato hexadecimal
					System.out.println("Respuesta del servidor (certificado): " + fromServer);	
				}

				//Se parsea el formato hexadecimal del certificado del servidor a un formato de certificado X509
				byte[] certificadoBytes = DatatypeConverter.parseHexBinary(fromServer);
				CertificateFactory creador = CertificateFactory.getInstance("X.509");
				InputStream in =  new ByteArrayInputStream(certificadoBytes);
				X509Certificate certificadoServidor = (X509Certificate)creador.generateCertificate(in);
				
				//Se obtiene la llave publica del servidor a partir del certificado
				llavePublicaServidor = certificadoServidor.getPublicKey();
				
				System.out.println();
				estado++;
			}//end case_2
			case 3:{

				//Se crea un generador de llaves simetricas a partir del algoritmo simetrico, con llaves de 128 bits
				KeyGenerator keygen = KeyGenerator.getInstance(algoritmoSimetrico);
				keygen.init(128);
				//Se crea la llave simetrica
				llaveSimetrica = keygen.generateKey();
				//Se obtiene la llave simetrica en formato de bytes
				byte[] llaveSimetricaBytes = llaveSimetrica.getEncoded();
				
				//Se cifra la llave simetrica con la llave publica del servidor para asegurar su confidencialidad
				byte[] llaveSimetricaCifrada = Asimetrico.cifrar(llavePublicaServidor, algoritmoAsimetrico, llaveSimetricaBytes);
				String stringHexLLaveSimetricaCifrada = DatatypeConverter.printHexBinary(llaveSimetricaCifrada);

				//Se envia la llave simetrica cifrada
				System.out.println("Envio llave simetrica cifrada: " + stringHexLLaveSimetricaCifrada);
				pOut.println(stringHexLLaveSimetricaCifrada);

				if((fromServer = pIn.readLine()) != null){
					//Se recibe la llave simetrica cifrada con la llave publica del cliente, la cual
					//el servidor obtiene del certificado anteriormente enviado
					System.out.println("Respuesta del servidor (llave simetrica cifrada): " + fromServer);	
				}

				//Se descifra la llave enviada por el servidor con la llave privada del cliente
				byte[] llaveSimetricaDescifrada = Asimetrico.descifrar(llavePrivada, algoritmoAsimetrico, DatatypeConverter.parseHexBinary(fromServer));

				//Se valida que el servidor haya recibido correctamente la llave simetrica
				boolean llavesIguales = true;
				for(int i=0; i<llaveSimetricaBytes.length; i++){
					llavesIguales = llavesIguales && llaveSimetricaBytes[i] == llaveSimetricaDescifrada[i];
				}

				if(llavesIguales){
					//Se le avisa al servidor de que se mantuvo la integridad de la llave simetrica
					pOut.println("OK");
					System.out.println("La llave simetrica fue enviada correctamente, se mantuco su integridad");
				}else{
					System.out.println("ERROR: La llave simetrica no fue enviada/recibida correctamente");
					break;
				}
				
				System.out.println();
				estado++;

			}//end_case_3

			case 4:{
				
				//Se crean datos por defecto de las coordenadas del cliente
				String datos = "15;41 24.2028,2 10.4418";
				//Se cifran estos datos con la llave simetrica
				byte[] datosCifrados = Simetrico.cifrar(llaveSimetrica, algoritmoSimetrico, datos);
				String stringHexDatosCifrados = DatatypeConverter.printHexBinary(datosCifrados);

				System.out.println("Envio datos cifrados: " + stringHexDatosCifrados);
				//Se encian los datos cifrados simetricamente de las coordenadas del cliente
				pOut.println(stringHexDatosCifrados);

				//Se crea e inicializa el algoritmo HMAC especificado
				HMac hmac = null;
				if(algoritmoHMAC.equalsIgnoreCase("HMACSHA1")){
					hmac = new HMac(new SHA1Digest());
				}
				else if(algoritmoHMAC.equalsIgnoreCase("HMACSHA256")){
					hmac = new HMac(new SHA256Digest());
				}
				else if(algoritmoHMAC.equalsIgnoreCase("HMACSHA384")){
					hmac = new HMac(new SHA384Digest());
				}
				else if(algoritmoHMAC.equalsIgnoreCase("HMACSHA512")){
					hmac = new HMac(new SHA512Digest());
				}

				//Se le asigna la llave simetrica al HMAC
				hmac.init(new KeyParameter(llaveSimetrica.getEncoded()));

				//Se obtienen los datos de las coordenadas en formato de byte array
				byte[] byteArrayDatos = datos.getBytes();
				
				//Se crea una variable hashBytes que sera el resultado del algoritmo HMAC aplicado a los datos
				byte[] hashBytes = new byte[hmac.getMacSize()];
				hmac.update(byteArrayDatos, 0, byteArrayDatos.length);
				hmac.doFinal(hashBytes, 0);

				//Se obtiene el hash resultado del HMAC en formato hexadecimal
				String hashHex = DatatypeConverter.printHexBinary(hashBytes);
				System.out.print("Hash bytes: ");
				BouncyCastle.imprimirByteArray(hashBytes);
				System.out.println("Hash Hexadecimal: " + hashHex);
				
				//Se envia el hash de los datos en formato hexadecimal al servidor
				pOut.println(hashHex);

				if((fromServer = pIn.readLine()) != null){
					//Se obtiene el hash de los datos cifrado con la llave privada del servidor
					//Esto con el fin de autenticar al servidor y confirmar que se mantuvo la integridad de los datos
					System.out.println("Respuesta del servidor (hash cifrado): " + fromServer);	
				}

				//Se obtiene el hash cifrado recibido del servidor en formato de bytes
				byte[] hashCifrado =  DatatypeConverter.parseHexBinary(fromServer);
				//Se descifran los bytes del hash cifrado con la llave publica del servidor
				byte[] hashDescifrado = Asimetrico.descifrar(llavePublicaServidor, algoritmoAsimetrico, hashCifrado);

				System.out.print("Hash descifrado bytes: ");
				BouncyCastle.imprimirByteArray(hashDescifrado);

				//Se valida que los dos hash con iguales para confirmar la integridad de los datos
				boolean hashIguales = true;
				for(int i=0; i<hashBytes.length; i++){
					hashIguales = hashIguales && hashBytes[i] == hashDescifrado[i];
				}
				
				if(hashIguales){
					System.out.println("Los datos fueron enviados correctamente, se mantuvo su integridad");
				}else{
					System.out.println("ERROR: Los datos no fueron enviados correctamente");
				}
				estado++;
			}//end_case4

			}//end_switch

		}//end_while

	}//end_procesar
}
