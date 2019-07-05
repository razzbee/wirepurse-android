package com.transcodium.wirepurse.classes

class CryptoAddress {

    companion object {

        /**
         * validate, this is just to test if the pattern is
         * correct, real validation is redone on server side
         */
        fun isValid(
                chain: String,
                address: String,
                paymentId: String? = null
        ) : Boolean {

            val regex = when(chain){
                "btc" -> "^(bc1|[13])[a-zA-HJ-NP-Z0-9]{25,39}\$"
                "eth" -> "^(0x){1}[0-9a-fA-F]{40}\$"
                "xmr" -> "4[0-9AB][1-9A-HJ-NP-Za-km-z]{93}"
                "ltc" -> "[LM3][a-km-zA-HJ-NP-Z1-9]{26,33}"
                else -> return false
            }

            return regex.toRegex().matches(address)
        }//end fun

    }
}//end class