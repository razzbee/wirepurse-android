package com.transcodium.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.transcodium.wirepurse.R
import com.transcodium.wirepurse.classes.ListItemModel

interface OnItemClickListener {
    fun onClick(position: Int,
                view: View?,
                parent: ViewGroup
    )
}

/**
 * Created by dr_success on 12/13/2017.
 */
class LinearListAdapter(
        val ctx: Context,
        val data: MutableList<ListItemModel>,
        var listRowLayout: Int = R.layout.drawer_list_item_row
)
    : ArrayAdapter<ListItemModel>(ctx,listRowLayout,data) {

    //viewHolder Inner Class
     data class viewHolder(
                    var menuIcon: ImageView,
                    var menuTitle: TextView
    )

    //last position
    private var lastPosition = -1

    private val clickListeners: MutableList<OnItemClickListener> = mutableListOf()

    fun setOnItemClickListener(listener: OnItemClickListener){
        clickListeners.add(listener)
    }

    //getView
    override fun getView(position: Int,
                         convertView: View?,
                         parent: ViewGroup): View{

          //row data
          var rowData = getItem(position)

          var vh: viewHolder

          var rowView = convertView

          //sometimes convertView is empty
          if(rowView == null) {

              //if empty we should inflate view
              rowView = LayoutInflater.from(ctx)
                            .inflate(listRowLayout,parent,false)
          }//end if

         //lets set the data
         rowView!!.findViewById<ImageView>(R.id.itemIcon)
                 .setImageDrawable(ContextCompat.getDrawable(ctx,rowData.icon))

        rowView.tag = rowData.tagName

        //lets set title
        rowView.findViewById<TextView>(R.id.itemTitle)
                .text = rowData.title


        rowView.setOnClickListener {
            for(listener in clickListeners){
                listener.onClick(
                        position = position,
                        view = convertView,
                        parent = parent
                )
            }
        }

        return rowView
      }//end get view


}//end