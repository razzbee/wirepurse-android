package com.transcodium.wirepurse.classes

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TableRow
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject


interface BindViewHolderListner{
    fun onBind(
            dataSet: MutableList<JSONObject>,
            holder: TableViewRecycler.RViewHolder,
            position: Int
    )
}

class TableViewRecycler(
        val activity: Activity,
        var dataSet: MutableList<JSONObject>,
        val layoutRes: Int
) : RecyclerView.Adapter<TableViewRecycler.RViewHolder>() {

    private var bindViewHolderListener: BindViewHolderListner? = null

    fun setOnBindViewHolderListner(listner: BindViewHolderListner){
        this.bindViewHolderListener = listner
    }

    class RViewHolder(itemView: TableRow): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            TableViewRecycler.RViewHolder {
        val itemView = LayoutInflater
                .from(activity)
                .inflate(
                       layoutRes,
                        parent,
                        false
                ) as TableRow

        return TableViewRecycler.RViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TableViewRecycler.RViewHolder, position: Int) {
        bindViewHolderListener?.onBind(dataSet,holder,position)
    }

    fun dataSet(): MutableList<JSONObject>{
        return dataSet
    }

    fun replaceDataSet(data: MutableList<JSONObject>){
        dataSet = data
        notifyDataSetChanged()
    }

    fun clearDataSet() {
        dataSet = mutableListOf()
        notifyDataSetChanged()
    }

    fun appendData(data: MutableList<JSONObject>){
        dataSet.addAll(data)
        notifyDataSetChanged()
    }

    override fun getItemCount() = dataSet.size
}