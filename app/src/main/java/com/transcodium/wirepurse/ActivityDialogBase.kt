package com.transcodium.wirepurse

import android.os.Bundle
import android.os.PersistableBundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.transcodium.wirepurse.classes.AppAlert
import com.transcodium.wirepurse.classes.Status
import com.transcodium.wirepurse.classes.errorStatus
import kotlinx.android.synthetic.main.circular_progress_bar.*
import kotlinx.coroutines.launch

open class ActivityDialogBase  : RootActivity()  {

    val baseActivity by lazy{
        this
    }

    override fun onStart() {
        super.onStart()

        val win = window

        val maxWidth = toDip(this,380f).toInt()

        if(win != null) {


            val lp = WindowManager.LayoutParams()

            lp.copyFrom(win.attributes)

            var dialogWidth = maxWidth

            val curWidth = lp.width

            if (curWidth < maxWidth){
                dialogWidth = ViewGroup.LayoutParams.MATCH_PARENT
            }

            win.setLayout(
                    dialogWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }//end id

    }

    /**
     * error
     */
    fun opError(status: Status? = null) = UI.launch{

        val alertData = if(status != null){
            status
        }else{
            errorStatus(R.string.unexpected_error)
        }

        if(progressBar.isVisible){ progressBar.hide() }

        AppAlert(baseActivity).showStatus(alertData)
    }
}