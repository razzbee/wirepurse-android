package com.transcodium.wirepurse

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import androidx.core.view.get
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_notifications.*
import com.transcodium.wirepurse.classes.NotificationCore
import com.transcodium.wirepurse.classes.getData
import com.transcodium.wirepurse.classes.isEmpty
import com.transcodium.wirepurse.db.entities.Notifications
import com.transcodium.wirepurse.view_models.NotificationViewModel
import kotlinx.android.synthetic.main.notification_list_layout.view.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import com.transcodium.wirepurse.db.AppDB
import kotlinx.android.synthetic.main.circular_progress_bar.*
import kotlinx.android.synthetic.main.reload_content_view.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject


class NotificationsActivity : AppCompatActivity(){

    val mActivity by lazy { this }

    val seenNotifsIds = mutableListOf<String>()

    private var totalNotifDataInDB = 0

    val notificationCore by lazy {
        NotificationCore()
    }

    lateinit var recycleViewAdapter: notificationListAdapter

    val notificationDao by lazy {
        AppDB.getInstance(this)
                .notificationDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_notifications)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        toolbar.setNavigationOnClickListener {
            this.onBackPressed()
        }

        //initial pull data from network
        IO.async {
           NotificationCore()
                    .fetchAndProcessNotifications(mActivity,false)
        }


        pullToRefreshView.setLoaderColors(mActivity)

        pullToRefreshView.setOnRefreshListener {
            reloadDataFromNetwork()
        }

        reloadContentButton.setOnClickListener {
            reloadContentView.hide()
            reloadDataFromNetwork()
        }

        //attach swipe menu
        ItemTouchHelper(SwipeController()).attachToRecyclerView(recyclerView)

         recycleViewAdapter = notificationListAdapter(mActivity)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = recycleViewAdapter



        //start observer
        val viewProvider = ViewModelProviders.of(this)
                .get(NotificationViewModel::class.java)

        viewProvider.fetchLatest().observe(mActivity, Observer { itemsList ->

            progressBar.hide()

            if(itemsList.isEmpty()){
                reloadContentText.text = getString(R.string.no_notification_available)
                reloadContentView.show()
            }


            totalNotifDataInDB = itemsList.size

            //process set Seen
            processSetSeen()

            if (!itemsList.isEmpty()){

                reloadContentView.hide()

                recycleViewAdapter.setData(itemsList.toMutableList())


                //get unseen and set it seen
                itemsList.map {item ->

                    if(item.seen != 1){
                        seenNotifsIds.add(item.remoteId)
                    }
                }
            } //end if not empty

        })
    } //end on create


    /**
     * onCreatOptionsMenu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.notifications_menu,menu)

        for(i in 0 until menu.size()){

            val menuItem = menu[i]

            menuItem.setOnMenuItemClickListener {menuItem->

                if(menuItem.itemId == R.id.refresh){
                    reloadDataFromNetwork()
                }

                return@setOnMenuItemClickListener  true
            }

        }//end for loop

        return true
    }

    /**
     * setSeen
     */
    private fun processSetSeen() {

        IO.async {

            if (seenNotifsIds.isEmpty()) {
               // println("--Not Seen empty")
                return@async
            }

            val setSeen = notificationCore.setSeen(mActivity, seenNotifsIds)

            if (setSeen.isSuccess()) {
                seenNotifsIds.clear()
            }
        }
    } //end un

    /**
     * save Seen Notfications here
     */
    var onPuasedCalled = false

    override fun onPause() {

        onPuasedCalled = true

        processSetSeen()
        super.onPause()
    }

    override fun onStop() {

        if(!onPuasedCalled) {
            processSetSeen()
        }

        super.onStop()
    }

    /**
     * refreshData
     */
    private  fun reloadDataFromNetwork() = IO.launch {

        processSetSeen()

        UI.launch { pullToRefreshView.isRefreshing = true }

        val status = NotificationCore().fetchAndProcessNotifications(
                mActivity, false
        )

        UI.launch {

            val data = status.getData() ?: JSONArray()

            if((status.isError() || data.isEmpty()) && totalNotifDataInDB == 0){
                reloadContentText.text = getString(R.string.no_notification_available)
                reloadContentView.show()
            }

            pullToRefreshView.isRefreshing = false
        }
    } //end fun


    /**
     * recyclerview adapter
     */
    inner class notificationListAdapter(
            val mContext: Context,
            var itemsList: MutableList<Notifications> = mutableListOf()
    ) : RecyclerView.Adapter<notificationListAdapter.ViewHolder>(){



       inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

           val notificationMessageView = itemView.notifMessageView

           val notificationTitleView = itemView.notifTitleView

       }


        fun getItems(): List<Notifications>{
            return itemsList
        }


        fun getItem(position: Int): Notifications {
            return itemsList[position]
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
           val layout = LayoutInflater.from(mContext)
                   .inflate(R.layout.notification_list_layout,parent,false)

            return ViewHolder(layout)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val item = itemsList[position]

            val itemView = holder.itemView

            itemView.id = position

            val seen = item.seen

            val itemDataJsonObj = JSONObject(item.data)

            val notificationText = itemDataJsonObj.getString("message")

            //val notifId = item.id!!

            //val notificationType = itemDataJsonObj.getString("type")

            val notificationTitle = itemDataJsonObj.getString("title")

            val formattedDateTime = epochMillisToTime(mActivity,item.createdAt)

            itemView.notifDateTime.text = (formattedDateTime)

            val newNotifIcon = itemView.newNotifIcon

            val newNotifText = itemView.newNotifText

            //if seen
            if(seen != 1){
                newNotifIcon.visibility = View.VISIBLE
                newNotifText.visibility = View.VISIBLE
            } else {
                newNotifIcon.visibility = View.GONE
                newNotifText.visibility = View.GONE
            }

            holder.notificationMessageView.text = notificationText
            holder.notificationTitleView.text = notificationTitle

        }

        override fun getItemCount(): Int {
           return itemsList.size
        }


        /**
         * setData
         */
        fun setData(data: MutableList<Notifications>){
            itemsList = data
            notifyDataSetChanged()
        }

        /**
         * deleteItem
         */
        fun deleteItem(position: Int,notifyChange: Boolean = true): Boolean{

            if(!itemsList.indices.contains(position)){
                return false
            }

            val itemToDelete = itemsList[position]

             itemsList.removeAt(position)

            notifyItemRemoved(position)

            IO.launch { notificationDao.deleteOne(itemToDelete.id!!) }

            return true
        }

    } //end inner class


    /**
     * SwipeController
     */
    inner class SwipeController(
            dragDirection: Int = 0,
            swipeDirection: Int = ItemTouchHelper.LEFT
    ) : ItemTouchHelper.SimpleCallback(
            dragDirection,
            swipeDirection
    ) {


        val deleteDelay = 6000L

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }


        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            viewHolder as notificationListAdapter.ViewHolder

            val itemView = viewHolder.itemView

            val countDownTimerTv = itemView.countDownTimerTv

            itemView.deleteItemLabelParent.visibility = View.GONE

            itemView.notificationItemView.visibility = View.GONE

            itemView.undoDeleteParent.visibility = View.VISIBLE

            itemView.notificationItemView.visibility = View.GONE

            recycleViewAdapter.notifyDataSetChanged()

            UI.launch {


                val timer = object: CountDownTimer(deleteDelay,1000L){

                    override fun onTick(millisUntilFinished: Long) {

                        val remainingInSecs = millisUntilFinished / 1000

                        countDownTimerTv.text = ("($remainingInSecs)")
                    }

                    override fun onFinish() {

                        recycleViewAdapter.deleteItem(viewHolder.adapterPosition)


                        itemView.deleteItemLabelParent.visibility = View.VISIBLE

                        itemView.notificationItemView.visibility = View.VISIBLE

                        itemView.undoDeleteParent.visibility = View.GONE
                    }

                }.start() //end count down


                itemView.undoDeleteCard.setOnClickListener {

                    itemView.deleteItemLabelParent.visibility = View.VISIBLE

                    itemView.notificationItemView.visibility = View.VISIBLE

                    itemView.undoDeleteParent.visibility = View.GONE

                    recycleViewAdapter.notifyItemChanged(viewHolder.adapterPosition)

                    //cancel twice due to some wiered behaviour detected
                    timer.cancel()
                    timer.cancel()

                    this.cancel()
                }

            }//en coroutine


        }//end onSwipe


        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {

            if(viewHolder != null) {

                val view = viewHolder.itemView.notificationItemView

                getDefaultUIUtil().onSelected(view)
            }
        }

        override fun onChildDrawOver(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder?, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {

            val view = viewHolder!!.itemView.notificationItemView

            getDefaultUIUtil()
                    .onDrawOver(c, recyclerView, view, dX, dY, actionState, isCurrentlyActive)
        }


        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {

            val view = viewHolder!!.itemView.notificationItemView

            getDefaultUIUtil()
                    .onDraw(
                        c,
                        recyclerView,
                        view,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )

            }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {

            val view = viewHolder.itemView.notificationItemView

            getDefaultUIUtil().clearView(view)
        }

    }//end  inner class


}//end class
