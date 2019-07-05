package com.transcodium.wirepurse.classes

import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.security.keystore.KeyExpiredException
import androidx.core.os.CancellationSignal
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.hardware.fingerprint.FingerprintManagerCompat.AuthenticationResult
import android.security.keystore.KeyProperties
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.annotation.RequiresApi
import com.transcodium.wirepurse.R
import java.security.*
import java.security.spec.ECGenParameterSpec




class FingerprintCore(val ctx: Context) {

    val fm by lazy{ FingerprintManagerCompat.from(ctx) }


    val KEY_NAME = "tnsMoneyFingerPrint"

    /**
     * hasHardware
     */
    fun hasHardware(): Boolean {

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return false
        }

        return fm.isHardwareDetected
    }

    /**
     * isEnabled
     */
    fun isEnabled(): Boolean {

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return false
        }

        return fm.hasEnrolledFingerprints()
    }


    /**
     * keyPair
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun generateKeyPair(): KeyPair{

        //create a cipher
        val keyPairGen = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore"
        )

        val keygenParamSpec = KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_SIGN)
                                    .setDigests(KeyProperties.DIGEST_SHA256)
                                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                                    .setUserAuthenticationRequired(true)

        /*/only SDK api 24 (nuogat) +
        //this makes it less secure, what if there is a change in ownership???
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //we cannot use this as it requires api 23,we will do it manually
            keygenParamSpec.setInvalidatedByBiometricEnrollment(false)
        }*/

        keyPairGen.initialize(keygenParamSpec.build())

        return keyPairGen.generateKeyPair()
    }//end fun

    /**
     * create
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun create(): Status{

        //for api 28+, nothing must be done
        //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
          //  return successStatus()
        //}

        return try{
            generateKeyPair()
            successStatus()
        }catch(e: Exception){
            errorStatus(ctx.getString(R.string.fingerprint_init_failed))
        }
    }


    /**
     * handleUI
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun authenticate(
            onAuthSuccess: (AuthenticationResult?) -> Unit,
            onAuthError: (Int,String?) -> Unit,
            onAuthFailed: () -> Unit
    ): Status{


        return try {

            val signature = Signature.getInstance("SHA256withECDSA")

            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            val publicKey = keyStore.getCertificate(KEY_NAME).publicKey

            val privateKey = keyStore.getKey(KEY_NAME, null) as PrivateKey?

            if (publicKey == null || privateKey == null) {
                return errorStatus(ctx.getString(R.string.fingerprint_auth_failed))
            }


            val cancellationSignal = CancellationSignal()

            val authCallback = object : FingerprintManagerCompat.AuthenticationCallback() {
                /**
                 * on Auth Error
                 */
                override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errMsgId, errString)
                    onAuthError(errMsgId, errString?.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onAuthFailed()
                }

                override fun onAuthenticationSucceeded(result: AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    onAuthSuccess(result)
                }
            }//end callback


            signature.initSign(privateKey)

            val cryptoObject = FingerprintManagerCompat.CryptoObject(signature)

            fm.authenticate(
                    cryptoObject,
                    0,
                    cancellationSignal,
                    authCallback,
                    null
            )

            successStatus(data = cancellationSignal)

        }catch(e: java.lang.Exception) {

            return when (e) {

                //if fingerprint is changed or key expired
                is KeyPermanentlyInvalidatedException, is KeyExpiredException -> {

                    //recreate  fingerpring
                    create()

                    //re authenticate the data
                    authenticate(onAuthSuccess, onAuthError, onAuthFailed)
                }

                else -> errorStatus()

            }//end when

        }//end exceptions

    }//end fun

}//end class