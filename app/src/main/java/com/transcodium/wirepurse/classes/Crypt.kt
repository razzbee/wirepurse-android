/**
# Copyright 2018 - Transcodium Ltd.
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the  Apache License v2.0 which accompanies this distribution.
#
#  The Apache License v2.0 is available at
#  http://www.opensource.org/licenses/apache2.0.php
#
#  You are required to redistribute this code under the same licenses.
#
#  Project TNSMoney
#  @author Razak Zakari <razak@transcodium.com>
#  https://transcodium.com
#  created_at 16/08/2018
 **/

package com.transcodium.wirepurse.classes

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import javax.crypto.Cipher
import javax.security.auth.x500.X500Principal
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import com.transcodium.wirepurse.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.*
import java.security.spec.MGF1ParameterSpec
import java.util.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.*
import kotlin.collections.HashMap


class Crypt {

    init {
      //add spongy castle
        Security.addProvider(org.spongycastle.jce.provider.BouncyCastleProvider())
    }



    private val IS_ANDROID_M_OR_HIGHER = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

    private val IS_LOWER_THAN_ANDROID_M = !IS_ANDROID_M_OR_HIGHER

    private val SPONGY_CASTLE_PROVIDER = org.spongycastle.jce.provider.BouncyCastleProvider()

    private  val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"

    private  val KEY_ALGO = "RSA"

    private  val CERT_START_DATE = Calendar.getInstance()

    private  val CERT_END_DATE = CERT_START_DATE.apply{ add(Calendar.YEAR,20) }

    private  val IV_LEN_BITS = 96 // 12byte

    private   val SYMMETRIC_KEY_LEN_BITS = 128 // 16 byte

    private  val GCM_TAG_SIZE_BITS = 128 // 16 byte

    private  val SYMMETRIC_ALGO = "AES"

    private  val KEY_ALIAS by lazy {
        BuildConfig.APPLICATION_ID
    }

    private  val RSA_KEY_SIZE_BITS = 2048

    private val mKeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
            load(null)
        }

    }//end ks


    //cipher transformation
    private val RSA_CIPHER_TRANSFORMATION by lazy {
        if(IS_LOWER_THAN_ANDROID_M){
            "RSA/ECB/PKCS1Padding"
        }else{
            "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        }
    }//end


    private  val AES_CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"


    /**
     * generateSymetric Key
     */
    private fun generateSymmetricKey(): SecretKey {

        val keygen = KeyGenerator.getInstance(
               SYMMETRIC_ALGO,
                SPONGY_CASTLE_PROVIDER
        )

        val srand = SecureRandom()

        keygen.init(SYMMETRIC_KEY_LEN_BITS, srand)

        val genKey = keygen.generateKey().encoded

        //secure key
        return SecretKeySpec(genKey, SYMMETRIC_ALGO)
    }//end fun


    /**
    * createSymetric Key
     */
    fun generateAsymmetricKey(context: Context):Status {

       // Log.e("KEY_ALIAS",generateSymetricKey(context).toJsonString())

        val generator = KeyPairGenerator.getInstance(KEY_ALGO, ANDROID_KEYSTORE_PROVIDER)


        val builder = if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){

            //api level lower than M (Mashmello)
            KeyPairGeneratorSpec.Builder(context)
                    .setAlias(KEY_ALIAS)
                    .setSerialNumber(BigInteger.ONE)
                    .setSubject(X500Principal("CN=$KEY_ALIAS"))
                    .setStartDate(CERT_START_DATE.time)
                    .setKeySize(RSA_KEY_SIZE_BITS)
                    .setEndDate(CERT_END_DATE.time)
                    .build()



        }else{ // ice cream mr and higher


            KeyGenParameterSpec
                    .Builder(
                            KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or
                            KeyProperties.PURPOSE_DECRYPT
                    )
                    .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .setCertificateSubject(X500Principal("CN=$KEY_ALIAS"))
                    .setCertificateNotBefore(CERT_START_DATE.time)
                    .setCertificateNotAfter(CERT_END_DATE.time)
                    .setKeySize(RSA_KEY_SIZE_BITS)
                    .setUserAuthenticationRequired(false)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build()
        }//end if

       val keyPair = try {
            generator.initialize(builder)
            generator.genKeyPair()
        }catch(e: Exception){
            Log.e("CREATE_KEYPAIR_ERROR","Failed to create keypair")
            e.printStackTrace()
            return errorStatus("security_keys_create_failed")
        }

        //Log.e("PRIVATE_KEY",generator.genKeyPair().toString())

        return successStatus(data = keyPair)
    }//end fn

    /**
     * fetchKey
     */
    private fun getAsymmetricKeyPair(): KeyPair?{

        val privateKey = mKeyStore.getKey(KEY_ALIAS,null) as PrivateKey?
        val publicKey = mKeyStore.getCertificate(KEY_ALIAS)?.publicKey

        return if (privateKey != null && publicKey != null) {
            KeyPair(publicKey, privateKey)
        } else {
            null
        }
    }//end  get Asymetric Key Pair

    /**
     * RSACipher
     */
    private fun  RSACipher(mode: Int,key: Key): Cipher{

        val cipher = Cipher.getInstance(RSA_CIPHER_TRANSFORMATION)

        if (IS_ANDROID_M_OR_HIGHER) {

            val OAEPSpec = OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec("SHA-1"),
                    PSource.PSpecified.DEFAULT)

            cipher.init(mode, key,OAEPSpec)

        } else {
            cipher.init(mode, key)
        }

        return cipher
    }//end

    /**
     * encrypt data
     */
     private fun RSAWrapKey(
            context: Context,
            data: SecretKey
    ): Status {

        //Log.e("Size",data.encoded.size.toString())

        //lets fetch the key
        var keyPair = getAsymmetricKeyPair()

        if(keyPair == null){

            //generate new keypair
            val generateKeyPair = generateAsymmetricKey(context)

            if(generateKeyPair.isError()){
                return generateKeyPair
            }

            keyPair = generateKeyPair.getData<KeyPair>()

            if(keyPair == null){
                Log.e("SECURITY_KEYPAIR_ERROR","Failed to create or fetch encryption keypair")
                return errorStatus(
                        message = R.string.failed_to_generate_security_keys,
                        isSevere = true
                )
            }
        }


        return try {

            val cipher = RSACipher(Cipher.WRAP_MODE,keyPair.public)

            val result = cipher.wrap(data)


            successStatus(data = result)

        }catch (e: Exception){
            errorStatus(
                    message = "rsa_data_encryption_failed",
                    code = StatusCodes.RSA_ENCRYPTION_FAILED,
                    isSevere = true
            )
        }

    }//en encrypt


    /**
     * encrypt data
     */
    private fun RSAUnwrapKey(
            data: Any
    ): Status {

        //lets fetch the key
        val keyPair = getAsymmetricKeyPair()

        if (keyPair == null) {
            return errorStatus("data_decryption_error")
        }


        val encryptedDataBytes = when(data){
            is String -> Base64.decode(data,Base64.DEFAULT)
            is ByteArray -> data

            else -> return errorStatus("unknown_encrypted_data")
        }


        val result = try {

            //cipher
            val cipher = RSACipher(Cipher.UNWRAP_MODE,keyPair.private)


            val unwrapedKey = cipher.unwrap(
                    encryptedDataBytes,
                    "AES",
                    Cipher.SECRET_KEY
            )


            successStatus(data = unwrapedKey)
        }catch(e: Exception){

            Log.e("RSA_KEY_UNWRAP_ERROR",
                    "${e.cause}" )

            e.printStackTrace()

            errorStatus(
                    message = "data_decryption_failed_1",
                    code = StatusCodes.APP_KEY_DECRYPTION_FAILED,
                    isSevere = true
            )
        }//end


        return result
    }//end fun


    /**
     * getIV
     */
    private fun generateIV(): ByteArray{

        val srand = SecureRandom()

        val iv = ByteArray(IV_LEN_BITS.fromBitToByte())// 16 byte which is 128 bit

        srand.nextBytes(iv)

        return iv
    }//end

    /**
     * createAPPKey
     */
    fun createAppKey(context: Context): Status{

        //check if app key exists, if not create on
        val hasAppKey = context.sharedPref().contains(APP_KEY_NAME)

        if(hasAppKey){
            return successStatus()
        }

        return getDecryptedAppKey(context,true)
    }

    /**
     * fetch encryptedKeys
     */
    private fun getDecryptedAppKey(context: Context,
                           createIfNoKey: Boolean? = false
    ): Status{


        var decryptedKey: SecretKey

        var encryptedKeyBytes: ByteArray

        val sharedPref =  context.sharedPref()

        val encryptedKey = sharedPref.getString(APP_KEY_NAME,null)

        if(encryptedKey == null){

            //if do not create if not found
            if(!createIfNoKey!!){
                return errorStatus(
                        message = R.string.missing_app_key,
                        code = StatusCodes.MISSING_APP_KEY
                )
            }

            decryptedKey = generateSymmetricKey()

            val encrypteKeyStatus = RSAWrapKey(
                    context,
                    decryptedKey
            )

            if(encrypteKeyStatus.isError()){
                return encrypteKeyStatus
            }

            encryptedKeyBytes = encrypteKeyStatus.getData<ByteArray>()!!

            //lets save it
            val encryptedDataStr = Base64.encodeToString(
                    encryptedKeyBytes,
                    Base64.DEFAULT
            )

            sharedPref.edit{
                putString(APP_KEY_NAME,encryptedDataStr)
            }

        }else{


            val decodeEcryptedKeys = Base64.decode(
                    encryptedKey,
                    Base64.DEFAULT
            )

            val decryptKeyStatus = RSAUnwrapKey(decodeEcryptedKeys)

            if(decryptKeyStatus.isError()){
                return decryptKeyStatus
            }

            if(decryptKeyStatus.data() == null){
                return errorStatus("failed_to_get_decryption_keys")
            }

            decryptedKey =  try{
                decryptKeyStatus.getData<SecretKey>()!!
            } catch(e: Exception){
                e.printStackTrace()
                return errorStatus("failed_to_get_decryption_keys")
            }

        }//end if

        return successStatus(data = decryptedKey)

     }//end fun


    /**
    * encrypt
     */
    fun encrypt(
            context: Context,
            data: Any
    ): Status {


        val appKeyStatus = getDecryptedAppKey(context,true)

        if(appKeyStatus.isError()){
            return appKeyStatus
        }

        val decryptedKey = appKeyStatus.getData<SecretKey>()!!

        //generate IV
        val ivBytes = generateIV()

        val GCMSpec = GCMParameterSpec(GCM_TAG_SIZE_BITS,ivBytes)

        val cipher = Cipher.getInstance(
                AES_CIPHER_TRANSFORMATION,
                SPONGY_CASTLE_PROVIDER
        )

        //cipher
        cipher.init(Cipher.ENCRYPT_MODE,decryptedKey,GCMSpec)

        val dataToEncrypt = if(data is String){
            data.toByteArray(Charsets.UTF_8)
        }else{
            data as ByteArray
        }

        return  try {

           val encryptedData = cipher.doFinal(
                    dataToEncrypt
            )

            val byteOut = ByteArrayOutputStream()

            val dataHashMap = hashMapOf<String, ByteArray>(
                    "encrypted_data" to encryptedData,
                    "iv" to ivBytes
            )

            ObjectOutputStream(byteOut).writeObject(dataHashMap)


            //convert to base64 string
            val result = Base64.encodeToString(
                    byteOut.toByteArray(),
                    Base64.DEFAULT
            )


            successStatus(data = result)
        }catch (e: Exception){
            Log.e("AES_ENCRYPTION_ERROR",
                    "Failed to create AES Encryption: ${e.cause}")

            e.printStackTrace()

            errorStatus(
                    message = R.string.data_encryption_error,
                    isSevere = true
            )
        }

    }//end rsa encrypt

    /**
     * extractParts
     */
    fun extractParts(data: Any): HashMap<String,ByteArray>?{

        //now decode the data
        val decodedData = when(data) {
          is String  -> Base64.decode(data, Base64.DEFAULT)
          is ByteArray -> data
          else -> return null
        }

        val byteArrayIn = ByteArrayInputStream(decodedData)

        val objInput = ObjectInputStream(byteArrayIn)

         val dataHashMap = objInput.readObject()

        if(dataHashMap !is HashMap<*,*>){
            return null
        }

        return (dataHashMap as HashMap<String,ByteArray>)
    }//end

    /**
     * decryptData
     */
    fun decrypt(context: Context,data: String): Status {

        val decodedData = Base64.decode(data, Base64.DEFAULT)

        val dataParts = extractParts(decodedData)

        if(dataParts == null){
            return errorStatus("encrypted_data_part_extract_error")
        }

        val encryptedData: ByteArray = dataParts["encrypted_data"]!!
        //val encryptedKeys: ByteArray = dataParts["key"]!!
        val ivDataBytes: ByteArray = dataParts["iv"]!!

        val decryptKeyStatus = getDecryptedAppKey(context,false)

        if(decryptKeyStatus.isError()){
            return decryptKeyStatus
        }

        //keys
        val secretKey = decryptKeyStatus.getData<SecretKey>()

        val gcmSpec = GCMParameterSpec(
                GCM_TAG_SIZE_BITS,
                ivDataBytes
        )

       return try {

           val cipher = Cipher.getInstance(AES_CIPHER_TRANSFORMATION,
                   SPONGY_CASTLE_PROVIDER
           )

           cipher.init(Cipher.DECRYPT_MODE,secretKey,gcmSpec)

           val decryptedData = cipher.doFinal(encryptedData)

          //val result = decryptedData

           successStatus(data = decryptedData)

       }catch (e: Exception){

           Log.e("AES_DECRYPTION_FAILURE","AES Decryption Failed: ${e.cause}")

           e.printStackTrace()

            errorStatus("data_decryption_failed_error")
       }

    }//end fun

}///end class