package com.transcodium.wirepurse

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TableRow
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_transaction_history.*
import kotlinx.android.synthetic.main.circular_progress_bar.*
import kotlinx.android.synthetic.main.transaction_history_table_row.view.*
import kotlinx.coroutines.launch

import ru.alexbykov.nopaginate.paginate.NoPaginate
import java.lang.Exception
import ru.alexbykov.nopaginate.callback.OnRepeatListener
import androidx.recyclerview.widget.RecyclerView
import android.app.Dialog
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.transcodium.wirepurse.classes.*
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import kotlinx.android.synthetic.main.dialog_header.*
import kotlinx.android.synthetic.main.reload_content_view.*
import kotlinx.android.synthetic.main.transaction_history_filter.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import org.json.JSONArray
import org.json.JSONObject
import ru.alexbykov.nopaginate.item.ErrorItem
import java.text.SimpleDateFormat
import java.util.*


class TransactionHistoryActivity : AppCompatActivity() , DatePickerDialog.OnDateSetListener{

    val mActivity by lazy { this }

    private var cryptoId: String? = null

    private var curPageNo = 1
    private var totalPages: Int = 0
    private var isLoadingData = false

    private var recyclerViewAdapter: TableViewRecycler? = null

    private var paginationObj: NoPaginate? = null

    private val walletCore = WalletCore()

    val appAlert  by lazy { AppAlert(this) }

    val pb by lazy { progressBar }

    private var initialDataLoaded = false

    private val filterDialog by lazy { initFilterDialog() }

    private  var startDateTextInput: TextInputEditText? = null
    private  var endDateTextInput: TextInputEditText? = null
    private  var activeDateInput: String? = null

    private val filterOptions by lazy {
        mutableMapOf<String,Any?>(
             "start_date" to null,
             "end_date" to null,
             "mode" to  mutableListOf("internal","external"),
             "type" to mutableListOf("deposit","withdrawal"),
             "status" to mutableListOf("pending","processing","processed")
        )
    }

    //these objects are just for saving states
    var startDateObj: MutableMap<String,Int>? = null
    var endDateObj: MutableMap<String,Int>?  = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        val data = intent.extras

        val assetInfoStr = data?.getString("asset_info")
                ?:  this.onBackPressed()

        val assetInfo = JSONObject(assetInfoStr.toString())

        val assetSymbol = assetInfo.getString("symbol")
                         .toUpperCase()

        val assetName = assetInfo.getString("name")
                        .capitalize()

        val title = "$assetName ($assetSymbol)"

        assetNameTextView.text = title

        setSupportActionBar(toolbar)
        appBar.bringToFront()

        //supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        //go back
        goBack.setOnClickListener {
            this.onBackPressed()
        }


       reloadContentButton.setOnClickListener {
            Log.e("LOADMORE","===>LOL<====")
            refreshData()
       }


        setStatusBarColor(getColorRes(R.color.colorPrimaryDark))

        cryptoId = assetInfo.getString("_id")

        filterOptions.put("crypto_id", cryptoId)

        //load initial table
        initTxHistoryData()


        filterFab.setOnClickListener {
            filterDialog.show()
        }


        refreshBtn.setOnClickListener {
            reloadContentView.hide()
            refreshData()
        }


        filterDialog.dialogTitle?.text = getString(R.string.filter_result)

        filterDialog.closeModal?.setOnClickListener {
            filterDialog.hide()
        }

    }//end oncreate

    /**
     * filterDialog
     */
    private fun initFilterDialog(): Dialog {

        val dialog = Dialog(mActivity,R.style.Theme_AppCompat_Light_Dialog)
        dialog.setContentView(R.layout.transaction_history_filter)

        dialog.window?.setLayout(
             ViewGroup.LayoutParams.MATCH_PARENT,
             ViewGroup.LayoutParams.WRAP_CONTENT
        )

         startDateTextInput = dialog.startDateTextInput

        endDateTextInput = dialog.endDateTextInput

        startDateTextInput!!.disableSoftKeyboard()
        endDateTextInput!!.disableSoftKeyboard()

        startDateTextInput!!.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus){ v.performClick() }
        }

        endDateTextInput!!.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus){ v.performClick() }
        }

        //listen to startDate Tx Input Click
        startDateTextInput!!.setOnClickListener {

            val datePicker = if(startDateObj == null) {
                DatePickerDialog
                        .newInstance(mActivity)
            } else {
                DatePickerDialog
                  .newInstance(
                     mActivity,
                          startDateObj!!["year"]!!,
                          startDateObj!!["month"]!!,
                          startDateObj!!["day"]!!
                )
            }

            datePicker.locale = Locale.getDefault()
            datePicker.setOkColor(getColorRes(R.color.blueGreyL5))
            datePicker.setCancelColor(getColorRes(R.color.blueGreyL5))
            datePicker.setTitle(getString(R.string.start_date))

            //set maxDate limit
            if(endDateObj != null){

                val maxDate = Calendar.getInstance().apply {
                    set(endDateObj!!["year"]!!,endDateObj!!["month"]!!,endDateObj!!["day"]!!)
                }

                datePicker.maxDate = maxDate
            }//end if

            activeDateInput = "start_date"

            datePicker.show(supportFragmentManager,"StartDate")
        } //end datepicker

        //set end date
        endDateTextInput!!.setOnClickListener {

            activeDateInput = "end_date"

            //if already picked, set selected
            val datePicker = if(endDateObj == null) {
                DatePickerDialog
                        .newInstance(mActivity)
            } else {
                DatePickerDialog
                        .newInstance(
                                mActivity,
                                endDateObj!!["year"]!!,
                                endDateObj!!["month"]!!,
                                endDateObj!!["day"]!!
                        )
            }

            //set minDate limit
            if(startDateObj != null){

                val minDate = Calendar.getInstance().apply {
                    set(startDateObj!!["year"]!!,startDateObj!!["month"]!!,startDateObj!!["day"]!!)
                }

                datePicker.minDate = minDate
            }//end set min date


            datePicker.locale = Locale.getDefault()
            datePicker.setOkColor(getColorRes(R.color.blueGreyL5))
            datePicker.setCancelColor(getColorRes(R.color.blueGreyL5))

            datePicker.setTitle(getString(R.string.end_date))

            datePicker.show(supportFragmentManager,"EndDate")

        } //end end date text input datepicker

        //on ance btn clicked
        dialog.cancelBtn.setOnClickListener { dialog.hide() }

        //on okay btn click
        dialog.okBtn.setOnClickListener {

            reloadContentView.hide()

            //reInit Data
            initTxHistoryData()

            dialog.hide()
        }//end okBtn listener

        return dialog
    }//end fun

    //processFilterResult
    fun handleFilterCheckboxes(v: View){

        v as MaterialCheckBox

        val tag = v.tag.toString()

        if(!tag.contains("_")){
            return
        }

        val optionGroup = tag.substringBefore("_")

        val optionValue = tag.substringAfter("_")
                .toLowerCase()

        val isChecked = v.isChecked

        val optionGroupData: MutableList<String> = (filterOptions[optionGroup] as MutableList<String>?)
                ?: mutableListOf()

        if(isChecked){
           if(!optionGroupData.contains(optionValue)){
               optionGroupData.add(optionValue)
           }
        } else {

            if(optionGroupData.contains(optionValue)){
                optionGroupData.remove(optionValue)
            }
        }

        //replace the data
        filterOptions[optionGroup] = optionGroupData

        //println("===>> $filterOptions")
    }//end



    /**
     * onDateSet
     */
    override fun onDateSet(view: DatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int) {

        val data =  mutableMapOf(
                "year" to year,
                "month" to monthOfYear,
                "day" to dayOfMonth
        )

        val sdf =  SimpleDateFormat.getDateInstance()

        val cal = Calendar.getInstance()
            cal.set(year,monthOfYear,dayOfMonth)

        val dateStr = sdf.format(cal.time)

        val dateMillis = cal.timeInMillis

        if(activeDateInput == "start_date"){

            startDateObj = data

            startDateTextInput?.setText(dateStr)

            filterOptions["start_date"] = dateMillis
        } else {

            endDateObj = data

            endDateTextInput?.setText(dateStr)

            filterOptions["end_date"] = dateMillis
        }
    } //end listener

    /**
     * refreshData
     */
    fun refreshData() = UI.launch{

        if(isLoadingData){
            longToast(R.string.app_loading_data)
            return@launch
        }

        pb.show()

        initTxHistoryData().join()

        pb.hide()
    } //end

    //show reload content
    private fun showReloadContent(text: String){
        pb.hide()
        recyclerView.hide()
        reloadContentText.text = text
        reloadContentView.show()
    }

    /**
     * processesTxHistory
     */
    fun initTxHistoryData() = IO.launch {

        curPageNo = 1

        pb.show()

        val txHistoryStatus = walletCore.fetchTxHistory(
                mActivity,
                cryptoId!!,
                curPageNo,
                filterOptions
        )

        UI.launch {

            pb.hide()

            if(txHistoryStatus.isError()){
                showReloadContent(txHistoryStatus.getMessage(mActivity))
                return@launch
            }

            //println(txHistoryStatus.toJsonString())

            val allData = try{

                //sometimes content encoding issues causes
                // the return data to returned as
                //binary instead of JSONObject
                //fix that bug here

                val data = txHistoryStatus.data()

                val dataJSONObject = if(data is JSONObject){
                    data
                } else if(data is ByteArray){
                    JSONObject(String(data,Charsets.UTF_8))
                } else {

                    showReloadContent(getString(R.string.unexpected_error))
                    return@launch
                }

                dataJSONObject!!
            } catch (e: Exception){

                //println("===>>${txHistoryStatus.toJsonString()}")

                e.printStackTrace()

                showReloadContent(getString(R.string.unexpected_error))

                initialDataLoaded = false

                return@launch
            }

            totalPages = allData.optInt("total_pages",0)

            val totalData = allData.optInt("total_data",0)

            val dbData = allData.optJSONArray("db_data") as JSONArray?

            if (dbData == null || dbData.isEmpty() || totalData == 0) {

                if (curPageNo == 1) {
                    showReloadContent(getString(R.string.no_data_found))

                    initialDataLoaded = false
                }
                return@launch
            } //end if

            recyclerView.show()

            //lets convert the db data to mutable list
            val dbDataListData = dbData.toMutableList<JSONObject>()


            reloadContentView.hide()

            //if tthis fun has been loaded already
            if(initialDataLoaded) {

              recyclerViewAdapter?.replaceDataSet(dbDataListData)
              recyclerView.invalidate()

              //set initial data loaded
              initialDataLoaded = true

            } else {

                //lets process the data here
                recyclerViewAdapter = TableViewRecycler(
                        mActivity,
                        dbDataListData,
                        layoutRes = R.layout.transaction_history_table_row
                )

                //set initial data loaded
                initialDataLoaded = true

                val backgrounds = listOf(
                        ContextCompat.getColor(mActivity, R.color.blueGreyL5),
                        ContextCompat.getColor(mActivity, R.color.white)
                )

                recyclerViewAdapter!!.setOnBindViewHolderListner(object : BindViewHolderListner {
                    override fun onBind(
                            dataSet: MutableList<JSONObject>,
                            holder: TableViewRecycler.RViewHolder,
                            position: Int
                    ) {
                        //println("-----------------> $dataSet")
                        val backgroundColor = if (position % 2 == 0) {
                            backgrounds[0]
                        } else {
                            backgrounds[1]
                        }

                        val txItemObj = dataSet[position]

                        val itemId = txItemObj.getString("_id")

                        val assetName = txItemObj.optString("asset_name", "")

                        val assetSymbol = txItemObj.optString("asset_symbol", "")
                                .toUpperCase()

                        val createdAt = txItemObj.getLong("created_at")

                        val confirmations = txItemObj.optInt("confirmations", 0)

                        val txDateTime = createdAt.epochMillisUTCToLocalDateTime()

                        val amount = txItemObj.optDouble("amount", 0.0)

                        val txStatusText = txItemObj.optString("status_text", "")

                        val itemView = holder.itemView

                        val itemExistsTag = itemView.findViewWithTag<TableRow?>(itemId)

                        //if item exists, skip
                        if (itemExistsTag != null) {
                            return
                        }

                        itemView.tag = itemId

                        itemView.setBackgroundColor(backgroundColor)

                        itemView.date.text = txDateTime

                        itemView.amount.text = ("$amount $assetSymbol")

                        itemView.confirmation.text = confirmations.toString()

                        val txType = txItemObj.getString("tx_type")

                        val txTypeText = if (txType == "withdrawal") {
                            getString(R.string.withdrawal)
                        } else {
                            getString(R.string.deposit)
                        }

                        itemView.type.text = txTypeText

                        val txMode = txItemObj.getString("tx_mode")

                        val txModeText = if (txMode == "internal") {
                            getString(R.string.internal)
                        } else {
                            getString(R.string.external)
                        }

                        itemView.mode.text = txModeText

                        val fromAddress = txItemObj
                                .optString("from_address", getString(R.string.na))

                        itemView.from.text = fromAddress

                        val toAddress = txItemObj
                                .optString("to_address", getString(R.string.na))

                        itemView.to.text = toAddress


                        val txHash = txItemObj
                                .optString("tx_hash", getString(R.string.na))

                        itemView.hash.text = txHash

                        itemView.status.text = txStatusText
                    }
                })

                val lmanager = LinearLayoutManager(mActivity).apply{
                    orientation = RecyclerView.VERTICAL
                }

                recyclerView.layoutManager = lmanager

                //set adapter
                recyclerView.adapter = recyclerViewAdapter

            } //end if this method has been initiated already

            //unbind and rebind
            paginationObj?.unbind()

            paginationObj = NoPaginate.with(recyclerView)
                    .setOnLoadMoreListener { loadMore() }
                    .setCustomErrorItem(OnLoadError())
                    .build()

            if (totalPages == 0 || curPageNo == totalPages) {
                paginationObj!!.setNoMoreItems(true)
            } else {
                paginationObj!!.setNoMoreItems(false)
            }

        } //end UI thread

    } //end fun


    /**
     * loadMore
     */
    private fun loadMore(isRefresh: Boolean = false) = IO.launch {


        if(curPageNo == totalPages || totalPages == 0){
            paginationObj?.setNoMoreItems(true)
            return@launch
        }

        //next page is an increment
        val nextPage = curPageNo + 1

        UI.launch {

            if (!isRefresh) {
                pb.hide()
                paginationObj?.showLoading(true)
            }
        }

        val txHistoryStatus = walletCore.fetchTxHistory(
                mActivity,
                cryptoId!!,
                nextPage,
                filterOptions
        )


        UI.launch {

           if(isRefresh) {
                pb.hide()
           }


            if (txHistoryStatus.isError()) {

                //appAlert.showStatus(txHistoryStatus)
                toast(txHistoryStatus.getMessage(mActivity))

                isLoadingData = false

                paginationObj?.showError(true)

                return@launch
            }

            paginationObj?.showLoading(false)

            val allData = try {
                txHistoryStatus.getData<JSONObject?>()
            } catch (e: Exception) {

                e.printStackTrace()

                //println(txHistoryStatus.toJsonString())

                //appAlert.error(R.string.unexpected_error)
                paginationObj?.showError(true)


                isLoadingData = false


                return@launch
            }

            totalPages = allData?.optInt("total_pages",0) ?: 0

            val totalData = allData?.optInt("total_data",0) ?: 0

            val dbData = allData?.optJSONArray("db_data")

            if (allData == null || dbData == null || totalData == 0) {
                return@launch
            }

            curPageNo = nextPage

            val recyclerViewDataSet = recyclerViewAdapter?.dataSet

            //lets convert the db data to mutable list
            val dbDataListData = mutableListOf<JSONObject>()

            for(index  in (0..dbData.length())){

                val dataObj = dbData[index] as JSONObject

                if(recyclerViewDataSet != null &&
                        recyclerViewDataSet.contains(dataObj)){
                    continue
                }

                dbDataListData.add(dataObj)
            }//end for loop

            recyclerViewAdapter?.appendData(dbDataListData)

            isLoadingData = false

            if (totalPages == 0 || curPageNo == totalPages) {
                paginationObj!!.setNoMoreItems(true)
            } else {
                paginationObj!!.setNoMoreItems(false)
            }


        } // UI thread launch

    }//end fun


    //on network load error
    inner class OnLoadError : ErrorItem {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                    R.layout.reload_network_request,
                    parent, false
            )
            return object : RecyclerView.ViewHolder(view) {
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, repeatListener: OnRepeatListener?) {

            val itemView = holder.itemView

            itemView.visibility = View.VISIBLE

            val btnRepeat = itemView.findViewById<View>(R.id.retryBtn) as AppCompatButton

            btnRepeat.setOnClickListener{
                refreshData()
                itemView.visibility = View.GONE
            }
        }
    } //end fun

}//end class
