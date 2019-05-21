package install.util.encryptor.crypto

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

class RSAGoodMan {

    private static final int DEFAULT_KEY_SIZE = 2048;

    private static final String KEY_FACTORY_ALGORITHM = "RSA";

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private static final String CHARSET = "UTF-8";



    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        RSAGoodMan rsaman = new RSAGoodMan()
        String text = "암호화된 문자열 abcdefg hijklmn"

        KeyPair pair = rsaman.generateKeyPair()
        String publicKey = byteArrayToHex(pair.getPublic().getEncoded())
        String privateKey = byteArrayToHex(pair.getPrivate().getEncoded())

        rsaman.test(text, publicKey, privateKey)
        rsaman.test('hi Test Ya', publicKey, privateKey)
        rsaman.test('이거슨 테스트야', publicKey, privateKey)
        rsaman.test('하하aa 11테스트 하는중 12451번째', publicKey, privateKey)
        rsaman.test(' ㄹ%@52 ㅏ하하 도도여뱌쟈탸 ../ TEST /..; ', publicKey, privateKey)

        rsaman.test(text, privateKey, publicKey)
        rsaman.test('hi Test Ya', privateKey, publicKey)
        rsaman.test('이거슨 테스트야', privateKey, publicKey)
        rsaman.test('하하aa 11테스트 하는중 12451번째', privateKey, publicKey)
        rsaman.test(' ㄹ%@52 ㅏ하하 도도여뱌쟈탸 ../ TEST /..; ', privateKey, publicKey)
    }




    void test(text, publicKey, privateKey){
        String encrypted = new RSAGoodMan().encrypt(text, publicKey)
        String decrypted = new RSAGoodMan().decrypt(encrypted, privateKey)
        println " - text       : ${text}"
        println " - encrypted  : ${encrypted}"
        println " - decrypted  : ${decrypted}"
        assert text == decrypted
    }

    /**************************************************
     *
     *  KeyPair Generator
     *
     **************************************************/
    static KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException{
        //KeyPairGenerator generator = KeyPairGenerator.getInstance("DiffieHellman", "SunJCE"); Not an RSA key: DH
        //KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "SunRsaSign"); // OK
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "SunJSSE"); // OK
        generator.initialize(DEFAULT_KEY_SIZE, new SecureRandom()); // 여기에서는 2048 bit 키를 생성하였음
        return generator.generateKeyPair();
    }

    /**************************************************
     *
     *  Encrypt
     *
     **************************************************/
    String encrypt(String text, String publicKeyString) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING", "SunJCE");
        // Turn the encoded key into a real RSA public key.
        // Public keys are encoded in X.509.
        X509EncodedKeySpec ukeySpec = new X509EncodedKeySpec(hexToByteArray(publicKeyString));
        KeyFactory ukeyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = null;
        try {
            publicKey = ukeyFactory.generatePublic(ukeySpec);
//            System.out.println("pubKeyHex:" + byteArrayToHex(publicKey.getEncoded()));
        } catch (InvalidKeySpecException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // 공개키를 전달하여 암호화
        byte[] input = text.getBytes();
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] cipherText = cipher.doFinal(input);

        return byteArrayToHex(cipherText)
    }

    /**************************************************
     *
     *  Decrypt
     *
     **************************************************/
    String decrypt(String encryptedText, String privateKeyString) throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException{
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING", "SunJCE");
        // Turn the encoded key into a real RSA private key.
        // Private keys are encoded in PKCS#8.
        PKCS8EncodedKeySpec rkeySpec = new PKCS8EncodedKeySpec(hexToByteArray(privateKeyString));
        KeyFactory rkeyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = null;
        try {
            privateKey = rkeyFactory.generatePrivate(rkeySpec);
//            System.out.println("privKeyHex:" + byteArrayToHex(privateKey.getEncoded()));
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        // 개인키를 가지고있는쪽에서 복호화
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] plainText = cipher.doFinal(hexToByteArray(encryptedText));
        return new String(plainText)
    }

    /**************************************************
     *
     *  Util
     *
     **************************************************/
    // hex string to byte[]
    public static byte[] hexToByteArray(String hex) {
        if (hex == null || hex.length() == 0) {
            return null;
        }
        byte[] ba = new byte[hex.length() / 2];
        for (int i = 0; i < ba.length; i++) {
            ba[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return ba;
    }

    // byte[] to hex sting
    public static String byteArrayToHex(byte[] ba) {
        if (ba == null || ba.length == 0) {
            return null;
        }
        StringBuffer sb = new StringBuffer(ba.length * 2);
        String hexNumber;
        for (int x = 0; x < ba.length; x++) {
            hexNumber = "0" + Integer.toHexString(0xff & ba[x]);

            sb.append(hexNumber.substring(hexNumber.length() - 2));
        }
        return sb.toString();
    }

}
