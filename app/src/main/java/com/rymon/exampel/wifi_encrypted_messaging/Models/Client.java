package com.rymon.exampel.wifi_encrypted_messaging.Models;


//model for Client activity extent NetworkModel

public class Client extends NetworkModel
{
    private static final Client ourInstance = new Client();

    public static Client getInstance() {
        return ourInstance;
    }

    private Client()
    {

    }
    //set publicKey and privateKey from Client
    public void setProperty(String publicKey , String privateKey, boolean isSecureSupported ) {
        this.setPrivateKey(privateKey);
        this.setPublicKey(publicKey);
        this.setSecureSupported(isSecureSupported);
    }
}
