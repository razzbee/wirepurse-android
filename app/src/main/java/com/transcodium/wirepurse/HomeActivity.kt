package com.transcodium.wirepurse

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.firebase.jobdispatcher.Job
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.transcodium.app.LinearListAdapter
import com.transcodium.wirepurse.view_models.HomeViewModel
import com.transcodium.wirepurse.view_models.LiveDataTransport
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.circular_progress_bar.*
import kotlinx.android.synthetic.main.home_bottom_sheet.*
import kotlinx.android.synthetic.main.home_coin_info.*
import kotlinx.coroutines.*
import org.jetbrains.anko.toast

import com.transcodium.app.OnItemClickListener
import com.transcodium.wirepurse.classes.*
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.bottom_navbar.*
import kotlinx.android.synthetic.main.bottom_navbar.view.*
import org.jetbrains.anko.longToast
import org.json.JSONObject


class HomeActivity : DrawerActivity() {


    private val homeActivity by lazy {
        this
    }

    private val pb by lazy { progressBar }

    private var liveDataObserverStarted = false

    private val walletCore =  WalletCore()

    var currentAssetInfo: JSONObject? = null

    private val assetItemClickLiveData by lazy {
        ViewModelProviders.of(this)
                .get(LiveDataTransport::class.java)
    }

    private lateinit var mBottomSheetBehaviour: BottomSheetBehavior<View>

    private var appJob: Job? = null

    private var isObservingNotifCount: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {

        setContentView(R.layout.activity_home)

        super.onCreate(savedInstanceState)

        //hide by default
        pb.hide()

        //initialize Home data
        initHome()


        /**
         * receive
         */
        receiveAsset.setOnClickListener {
            sendOrReceiveAssetDialog(ReceiveCryptoAssetActivity::class.java)
        }//end on click


        /**
         * sendCryptoAsset
         */
        sendAsset.setOnClickListener {
            sendOrReceiveAssetDialog(SendCryptoAssetActivity::class.java)
        }

        //handle bottom tab
        //handleBottomNav(mActivity)
        /**
         * homeBottomSheet
         */
        initBottomSheet()

        handleBottomNav(
                mActivity,
                R.id.wallet,
                bottomNavView
        )

        swipeRefreshView.setLoaderColors(this)

        //on pull refresh
        swipeRefreshView.setOnRefreshListener { initHome() }

    }//end onCreate

    override fun onBackPressed() {
        confirmExitDialog(this)
    }

    /**
     * if resumed from another activity
     */
    override fun onResume() {
        //set the wallet nav as active
        setActiveNav(
                R.id.wallet,
                bottomNavView
        )
        super.onResume()
    }

    /**
     * hide bottom sheet
     */
    fun hideBottomSheet(){
        if(mBottomSheetBehaviour.state  == BottomSheetBehavior.STATE_EXPANDED){
            mBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    /**
     * initBottomSheet
     */
    fun initBottomSheet(){

         mBottomSheetBehaviour = BottomSheetBehavior
                                    .from(homeBottomSheet)

        mBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN

        mBottomSheetBehaviour.setBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback(){

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if(newState == BottomSheetBehavior.STATE_EXPANDED){
                    bottomSheetListView.requestFocus()
                }
            }
        })

        assetMoreOptions.setOnClickListener {
            when(mBottomSheetBehaviour.state){
                BottomSheetBehavior.STATE_HIDDEN,
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    mBottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
                }

                BottomSheetBehavior.STATE_EXPANDED -> {
                    mBottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }

        contentView.setOnClickListener { v ->
            hideBottomSheet()
        }

        /**
         * observe asset item change
         */
        assetItemClickLiveData.data.observeForever { assetDataJson ->

            hideBottomSheet()

            //set currentAssetInfo
            currentAssetInfo = assetDataJson

            updateBottomSheetData(assetDataJson)
        }

        bottomSheetListView.setOnFocusChangeListener { v, hasFocus ->
            if(!hasFocus){
                hideBottomSheet()
            }
        }
    }//end

    /**
     * bottomListMenu Items
     */
    fun getBottomListMenuItems(assetSymbol: String) : MutableList<ListItemModel>{

        return mutableListOf(
                ListItemModel(
                        tagName = "transaction_history",
                        title = getString(R.string.asset_transaction_history,assetSymbol),
                        icon = R.drawable.ic_list,
                        targetActivity =  TransactionHistoryActivity::class.java
                ),

                ListItemModel(
                        tagName = "buy_asset",
                        title = getString(R.string.buy_asset,assetSymbol),
                        icon = R.drawable.ic_shopping_bag,
                        targetActivity =  ComingSoonActivity::class.java
                ),

                ListItemModel(
                        tagName = "sell_asset",
                        title = getString(R.string.sell_asset,assetSymbol),
                        icon = R.drawable.ic_money_bag,
                        targetActivity =  ComingSoonActivity::class.java
                )
        )

    } //end fun

    /**
     * updateBottomSheetData
     */
    fun updateBottomSheetData(data: JSONObject){

        val assetName = data.getString("name")
        val assetSymbol = data.getString("symbol").toUpperCase()

        val titleText = "$assetName ($assetSymbol)"

        titleTv.text = (titleText)

        val listItems = getBottomListMenuItems(assetSymbol)

        val listAdapter = LinearListAdapter(
                ctx = this,
                data = listItems,
                listRowLayout = R.layout.bottom_sheet_list_layout
        )

        bottomSheetListView.adapter = listAdapter


        listAdapter.setOnItemClickListener(object: OnItemClickListener{

            override fun onClick(position: Int, view: View?, parent: ViewGroup) {

                val listItem = listItems[position]

                val listItemTitle = listItem.title

                val listItemIcon = listItem.icon

                val targetActivity = listItem.targetActivity

                val intentData = Bundle().apply {
                    putString("asset_info", currentAssetInfo.toString())
                    putString("title",listItemTitle)
                    putInt("icon",listItemIcon)
                }

                homeActivity.startClassActivity(
                        activityClass = targetActivity,
                        data = intentData
                )
            }

        })//end on Click


    }//end fun

    /**
     * sendOrReceiveAssetDialog
     */
    fun <T>sendOrReceiveAssetDialog(clazz: Class<T>){

        //lets get the current asset
        ///fix errrrrrror... crashes onload cos its empty
        val assetSymbol = coinInfoCard.tag

        if(assetSymbol == null){
            toast(R.string.app_loading_data)
            return
        }

        val data = Bundle().apply { putString("asset_symbol",assetSymbol.toString()) }

        startClassActivity(
                activityClass = clazz,
                clearActivityStack = false,
                data = data
        )

    }//end


    /**
     * observe Live Data
     */
    private fun observeLiveData() {

       val viewProvider = ViewModelProviders.of(this)
                        .get(HomeViewModel::class.java)


        viewProvider.getUserAssets()
             .observe(this, Observer{ userAsset->

                   if(userAsset == null || userAsset.isEmpty()){

                       //if empty, filled should be replaced with progress loader
                       pb.show()
                       return@Observer
                   }

                   val dataStr = userAsset.first().data

                   val dataJson = JSONObject(dataStr)

                   //update user asset list
                 walletCore.homeUpdateUserAssetList(homeActivity,dataJson)

        })//end observer


       viewProvider.getCryptoAssetStats()
               .observe(this, Observer {assetStats->

                   if(assetStats == null){
                       return@Observer
                   }

                   //lets get selected or active coin in the info card
                   val activeCoinSymbol = homeActivity.coinInfoCard.tag?.toString() ?: "tns"

                   walletCore.homeUpdateAssetLatestPriceAndGraph(
                           activity = homeActivity,
                           assetSymbol = activeCoinSymbol,
                           allStatsJsonStr = assetStats.data
                   )//end

               })

    }//end fun


    /**
     * doPeriodicTask
     */
      private  fun initHome() = IO.launch {

        //load db data first
        UI.launch {

            longToast(R.string.refreshing_assets_data)

            swipeRefreshView.isRefreshing = true

            if (!liveDataObserverStarted) {
                observeLiveData()
            }

            liveDataObserverStarted = true

        }.onJoin


        //now load network data
        //initial data assets fetch
        walletCore.networkFetchUserAssets(homeActivity)

        //fetch asset stats
        walletCore.pollNetworkAssetStats(homeActivity as Context)

        UI.launch {
            swipeRefreshView.isRefreshing = false

            if(pb.isVisible){ pb.hide() }
        }
    }//end fun


    /**
     * onCreatOptionsMenu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.home_menu,menu)


        //0 until size is same as 0..(size - 1)
        // means the last size wont be looped
        for(i in 0 until (menu.size())){

            val menuItem = menu[i]

            val menuView = menuItem.actionView

            when(menuItem.itemId){

                R.id.menu_notifications -> {

                    //only call this after the menu has been inflated
                    NotificationCore().processNotificationCount(homeActivity,menuView)

                    menuView.setOnClickListener {
                        homeActivity.startClassActivity(NotificationsActivity::class.java)
                    }

                } //end fun
            } //end when

        }//end for loop

        return true
    }//end fun


}//end class
