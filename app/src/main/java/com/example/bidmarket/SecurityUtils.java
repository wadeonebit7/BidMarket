package com.example.bidmarket;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2 con salt por cuenta. Se mantienen hashes antiguos solo para migrarlos al iniciar sesión. */
public final class SecurityUtils {
    private SecurityUtils(){}
    private static final int ITERACIONES=120000;
    public static String hashPassword(String password){
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
        return "pbkdf2$"+ITERACIONES+"$"+hex(salt)+"$"+hex(pbkdf2(password,salt,ITERACIONES));
    }
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
    public static boolean requiereMigracion(String guardado){return guardado!=null&&!guardado.startsWith("pbkdf2$");}
    private static byte[] pbkdf2(String password,byte[] salt,int iterations){
        char[] chars=password.toCharArray();PBEKeySpec spec=new PBEKeySpec(chars,salt,iterations,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
        catch(Exception e){throw new IllegalStateException("No se puede derivar la contraseña",e);}
        finally{spec.clearPassword();Arrays.fill(chars,'\0');}
    }
    private static String hex(byte[] a){StringBuilder s=new StringBuilder();for(byte b:a)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
    private static byte[] fromHex(String s){if((s.length()&1)==1)throw new IllegalArgumentException();byte[] out=new byte[s.length()/2];for(int k=0;k<out.length;k++){int hi=Character.digit(s.charAt(k*2),16),lo=Character.digit(s.charAt(k*2+1),16);if(hi<0||lo<0)throw new IllegalArgumentException();out[k]=(byte)(hi*16+lo);}return out;}
}
