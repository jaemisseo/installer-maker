package install.util.encryptor

import java.security.MessageDigest

class SHA1Util implements EncryptionUtil{

    public static void main(String[] args) throws Exception {
        //Info
        String content = "java12345^&*()하하호호"
        //Run
        SHA1Util util = new SHA1Util()
        String e = util.encrypt(content)
        String d = util.decrypt(e)
        //Log
        println content
        println e
        println d
    }

    public SHA1Util() {
    }

    public SHA1Util(String key) {
    }

    String key
    String salt
    String charset = 'UTF-8'
    long iterations = 0



    @Override
    String encrypt(String content) {
        StringBuffer sbuf = new StringBuffer()

        //Define
        MessageDigest digest = MessageDigest.getInstance("SHA-1")
        digest.reset()
        //Salt
        digest.update()

        //Encription
        byte[] contentBytes = digest.digest(content.getBytes(charset))

        //Iteration (Encription)
        for (int i=0; i<iterations; i++){
            digest.reset()
            contentBytes = digest.digest(contentBytes)
        }

        for(int i=0; i<contentBytes.length; i++){
            byte tmpStrByte = contentBytes[i]
            String tmpEncTxt = Integer.toString((tmpStrByte & 0xff) + 0x100, 16).substring(1)
            sbuf.append(tmpEncTxt)
        }

        return sbuf.toString()
    }

    @Override
    String decrypt(String encryptedContent) {
        return null
    }

}
