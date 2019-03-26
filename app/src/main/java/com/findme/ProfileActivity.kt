package com.findme

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity() {

    var from = ""

    var firstName: String = ""
    var lastName: String = ""
    var email: String = ""
    var password: String = ""
    var cellPhone: String = ""
    var currentCountry: String = ""
    var countryOfResidence: String = ""
    var jobTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initializeView()
        initializeData()
        setDataToView()
    }

    private fun initializeView() {
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }
    }

    private fun initializeData() {
        val sharedPref =  getSharedPreferences("NEAR_ME",Context.MODE_PRIVATE)
        from = intent.getStringExtra("from")

        if (from == "current") {
            firstName = sharedPref.getString("cfirstName", "")
            lastName = sharedPref.getString("clastName", "")
            email = sharedPref.getString("cemail", "")
            password = sharedPref.getString("cpassword", "")
            cellPhone = sharedPref.getString("ccellPhone", "")
            currentCountry = sharedPref.getString("ccurrentCountry", "")
            countryOfResidence = sharedPref.getString("ccountryOfResidence", "")
            jobTitle = sharedPref.getString("cjobTitle", "")
        } else {
            firstName = sharedPref.getString("pfirstName", "")
            lastName = sharedPref.getString("plastName", "")
            email = sharedPref.getString("pemail", "")
            password = sharedPref.getString("ppassword", "")
            cellPhone = sharedPref.getString("pcellPhone", "")
            currentCountry = sharedPref.getString("pcurrentCountry", "")
            countryOfResidence = sharedPref.getString("pcountryOfResidence", "")
            jobTitle = sharedPref.getString("pjobTitle", "")
        }
    }

    private fun setDataToView() {
        nameTextView.text = firstName + " " + lastName
        emailTextView.text = email
        cellPhoneTextView.text = cellPhone
        currentCountryTextView.text = currentCountry
        countryOfResidenceTextView.text = countryOfResidence
        jobTitleTextView.text = jobTitle
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}

