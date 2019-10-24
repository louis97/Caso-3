import javax.xml.bind.DatatypeConverter;

public class prueba {

	public static void main(String[] args) {
		System.out.println(DatatypeConverter.printBase64Binary(verficarLongCadena("hola1")));
	}

	public static byte[] verficarLongCadena(String cadena){ 
		
		while(cadena.length()%4!=0){
			cadena="0"+cadena;
			
		}
	return DatatypeConverter.parseBase64Binary(cadena);
}
}
