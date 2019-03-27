package com.findme

import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.MenuItem
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_user_log.*

class UserLogActivity : AppCompatActivity() {

    private val TAG = UserLogActivity::class.java.simpleName
    private var pDialog: SweetAlertDialog? = null
    private var itemDecorator: DividerItemDecoration? = null

    private val logs: ArrayList<Location> = ArrayList()

    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_log)

        initializeView()
        initializeData()
    }

    private fun initializeView() {
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }
        itemDecorator = DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL)
        itemDecorator!!.setDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.divider)!!)
        userLogListRecyclerView.addItemDecoration(itemDecorator!!)
    }

    private fun initializeData() {
        userEmail = intent.getStringExtra("userEmail")
        showPeoples()
        userLogListRecyclerView.layoutManager = LinearLayoutManager(this)
        userLogListRecyclerView.adapter = UserLogAdapter(logs) { location: Location -> userItemClicked(location) }
    }

    private fun userItemClicked(location: Location) {

    }

    private fun showPeoples() {

        pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        pDialog!!.progressHelper.barColor = Color.parseColor("#00bcd4")
        pDialog!!.titleText = "Loading"
        pDialog!!.setCancelable(false)
        pDialog!!.show()

        val database = FirebaseDatabase.getInstance().reference
        val ref = database.child("users")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (singleSnapshot in dataSnapshot.children) {
                    val user = singleSnapshot.getValue(User::class.java)
                    if (user?.email == userEmail) {
                        for (location in user?.locationLog!!) {
                            logs.add(location)
                        }
                        break
                    }
                }
                userLogListRecyclerView.adapter!!.notifyDataSetChanged()
                pDialog!!.cancel()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException())
            }
        })
    }

    public override fun onStart() {
        super.onStart()
    }

    public override fun onStop() {
        super.onStop()
    }

    override fun onBackPressed() {
        /*SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("Are you sure to exit?")
            .setConfirmText("Yes")
            .setCancelText("No")
            .setConfirmClickListener { sDialog ->
                sDialog.dismissWithAnimation()
                System.exit(0)
            }
            .show()*/
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
