package com.example.bidmarket.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Genera y verifica hashes de contraseñas con PBKDF2 y un valor aleatorio
 * distinto para cada hash. También reconoce el formato antiguo con SHA-256
 * para permitir su migración después de un inicio de sesión válido.
 */
public final class SecurityUtils {
    /**
     * Evita crear instancias de esta clase de utilidades.
     */
    private SecurityUtils(){}
    /**
     * Número de iteraciones que se usa al generar un hash nuevo.
     */
    private static final int ITERACIONES=120000;
    /**
     * Genera un hash PBKDF2 con un valor aleatorio nuevo y guarda los datos
     * necesarios para verificar la contraseña más adelante.
     *
     * @param password contraseña que se quiere proteger. No debe ser nula.
     * @return cadena con el formato {@code pbkdf2$iteraciones$salt$hash}.
     * @throws NullPointerException si la contraseña es nula.
     * @throws IllegalStateException si no se puede derivar el hash.
     */
    public static String hashPassword(String password){
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
        return "pbkdf2$"+ITERACIONES+"$"+hex(salt)+"$"+hex(pbkdf2(password,salt,ITERACIONES));
    }
    /**
     * Comprueba la contraseña contra un hash PBKDF2 o un hash SHA-256 antiguo.
     *
     * @param password contraseña ingresada por el usuario.
     * @param almacenado hash guardado en la cuenta.
     * @return {@code true} si la contraseña coincide. Devuelve {@code false}
     *         si no coincide, no hay hash o no se puede verificar.
     */
    public static boolean verificar(String password,String almacenado){
        if(almacenado==null)return false;
        if(almacenado.startsWith("pbkdf2$")){
            try{String[] p=almacenado.split("\\$");if(p.length!=4)return false;
                int i=Integer.parseInt(p[1]);if(i<=0||i>1000000)return false;
                return MessageDigest.isEqual(pbkdf2(password,fromHex(p[2]),i),fromHex(p[3]));
            }catch(Exception e){return false;}
        }
        try{byte[] b=MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));return MessageDigest.isEqual(b,fromHex(almacenado));}
        catch(Exception e){return false;}
    }
    /**
     * Indica si el hash guardado se debe reemplazar tras verificar la contraseña.
     * Solo revisa el prefijo. No comprueba si el hash es válido.
     *
     * @param guardado hash almacenado en la cuenta.
     * @return {@code true} si el hash no es nulo y no empieza por {@code pbkdf2$}.
     */
    public static boolean requiereMigracion(String guardado){return guardado!=null&&!guardado.startsWith("pbkdf2$");}
    /**
     * Deriva un valor de 256 bits con PBKDF2 y HMAC-SHA-256.
     *
     * @param password contraseña de la que se deriva el valor.
     * @param salt bytes que se combinan con la contraseña para obtener un hash distinto.
     * @param iterations número de iteraciones de la derivación.
     * @return bytes del valor derivado.
     * @throws NullPointerException si la contraseña o el salt son nulos.
     * @throws IllegalArgumentException si el salt está vacío o las iteraciones no son positivas.
     * @throws IllegalStateException si no se puede derivar el valor.
     */
    private static byte[] pbkdf2(String password,byte[] salt,int iterations){
        char[] chars=password.toCharArray();PBEKeySpec spec=new PBEKeySpec(chars,salt,iterations,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
        catch(Exception e){throw new IllegalStateException("No se puede derivar la contraseña",e);}
        finally{spec.clearPassword();Arrays.fill(chars,'\0');}
    }
    /**
     * Convierte bytes a una cadena hexadecimal en minúsculas.
     *
     * @param a bytes que se quieren convertir.
     * @return cadena con dos caracteres hexadecimales por byte.
     */
    private static String hex(byte[] a){StringBuilder s=new StringBuilder();for(byte b:a)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
    /**
     * Convierte una cadena hexadecimal en bytes.
     *
     * @param s cadena hexadecimal que se quiere convertir.
     * @return bytes representados por la cadena.
     * @throws IllegalArgumentException si la longitud es impar o hay un carácter
     *         que no representa un dígito hexadecimal.
     */
    private static byte[] fromHex(String s){if((s.length()&1)==1)throw new IllegalArgumentException();byte[] out=new byte[s.length()/2];for(int k=0;k<out.length;k++){int hi=Character.digit(s.charAt(k*2),16),lo=Character.digit(s.charAt(k*2+1),16);if(hi<0||lo<0)throw new IllegalArgumentException();out[k]=(byte)(hi*16+lo);}return out;}
}
