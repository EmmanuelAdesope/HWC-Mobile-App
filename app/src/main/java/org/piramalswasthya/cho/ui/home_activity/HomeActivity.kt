package org.piramalswasthya.cho.ui.home_activity

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.tabs.TabLayout
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.piramalswasthya.cho.CHOApplication
import org.piramalswasthya.cho.R
import org.piramalswasthya.cho.adapter.ViewPagerAdapter
import org.piramalswasthya.cho.database.shared_preferences.PreferenceDao
import org.piramalswasthya.cho.databinding.ActivityHomeBinding
import org.piramalswasthya.cho.helpers.Languages
import org.piramalswasthya.cho.helpers.MyContextWrapper
import org.piramalswasthya.cho.list.benificiaryList
import org.piramalswasthya.cho.model.PatientListAdapter
import org.piramalswasthya.cho.repositories.UserRepo
import org.piramalswasthya.cho.ui.abha_id_activity.AbhaIdActivity
import org.piramalswasthya.cho.ui.home.SyncBottomSheetOverallFragment
import org.piramalswasthya.cho.ui.login_activity.LoginActivity
import org.piramalswasthya.cho.ui.outreach_activity.OutreachActivity
import org.piramalswasthya.cho.utils.AutoLogoutReceiver
import org.piramalswasthya.cho.work.WorkerUtils
import org.piramalswasthya.cho.work.WorkerUtils.amritSyncInProgress
import org.piramalswasthya.cho.work.WorkerUtils.downloadSyncInProgress
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private val MY_PERMISSIONS_REQUEST_CAMERA_AND_MIC = 1001

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WrapperEntryPoint {
        val preferenceDao: PreferenceDao
    }
    override fun attachBaseContext(newBase: Context) {
        val pref = EntryPointAccessors.fromApplication(
            newBase,
            WrapperEntryPoint::class.java
        ).preferenceDao
        super.attachBaseContext(
            MyContextWrapper.wrap(
            newBase,
            newBase.applicationContext,
            pref.getCurrentLanguage().symbol
        ))
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private lateinit var pager: ViewPager2 // creating object of ViewPager
    private lateinit var tab: TabLayout  // creating object of TabLayout
    private lateinit var homeAdapter: ViewPagerAdapter  // creating object of Adapter

    private lateinit var selectedLanguage: String
    private lateinit var currentLanguage: Languages
    private lateinit var currentRoleSelected: String
    private lateinit var currentSwitchRoleSelected: String
    private var selectedLanguageIndex: Int = 0
    private val languages = arrayOf("English", "ಕನ್ನಡ")

    private var _binding: ActivityHomeBinding? = null

    private val binding: ActivityHomeBinding
        get() = _binding!!
    private lateinit var navController: NavController

    private val viewModel: HomeActivityViewModel by viewModels()
    @Inject
    lateinit var prefDao: PreferenceDao

    @Inject
    lateinit var userRepo: UserRepo

    private var myLocation: Location? = null
    private var myInitialLoc: Location? = null
    private var locationManager: LocationManager? = null

    private var locationListener: LocationListener? = null

    var handler: Handler = Handler()
    var runnable: Runnable? = null
    var delay = 30000

    private val REQUEST_CODE_PERMISSION = 123

    private val syncBottomSheet: SyncBottomSheetOverallFragment by lazy {
        SyncBottomSheetOverallFragment()
    }

    override fun onResume() {
        handler.postDelayed(Runnable {
            handler.postDelayed(runnable!!, delay.toLong())
            Log.v("resuming activitiy", "resume")
            if( !amritSyncInProgress && !downloadSyncInProgress ){
//                downloadSyncInProgress = true
                viewModel.triggerDownSyncWorker(this, WorkerUtils.syncPeriodicDownSyncWorker)
            }
        }.also { runnable = it }, delay.toLong())
        super.onResume()
    }
    override fun onPause() {
        super.onPause()
        Log.v("pausing activitiy", "pause")
        handler.removeCallbacks(runnable!!)
    }

    private fun checkAndRequestPermissions() {
        // Check if the permission is not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            // Request the permissions
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), MY_PERMISSIONS_REQUEST_CAMERA_AND_MIC)
        } else {
            // Permissions are already granted
            // You can proceed with using the camera and microphone
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA_AND_MIC) {
            // Check if the permissions are granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                // Permissions granted, proceed with using the camera and microphone
            } else {
                // Permissions denied, handle accordingly (e.g., show a message, disable features)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions();
        _binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        (application as CHOApplication).addActivity(this)
        viewModel.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_PERMISSION)
            }
        }

//        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_PERMISSION)
//        }

        binding.syncButton.setOnClickListener {
            if (!syncBottomSheet.isVisible)
                syncBottomSheet.show(
                    supportFragmentManager,
                    resources.getString(R.string.sync)
                )
        }

        binding.refreshButton.setOnClickListener {
            Log.d("triggering down outside", "down trigger")
//            downloadSyncInProgress = true
            viewModel.triggerDownSyncWorker(this, WorkerUtils.syncOneTimeDownSyncWorker)
        }

        getCurrentLocation()
        setUpNavHeader()

        binding.translateButton.setOnClickListener {
            showLanguagePopUp()
        }

        drawerLayout = binding.drawerLayout
        navigationView = binding.navView

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_dehaze_24)

        // Tab part
        pager = binding.viewPager
        tab = binding.tabs
        val showDashboard = intent.getBooleanExtra("showDashboard", false)

        val dashboardBool = intent.extras?.getBoolean("dashboardBool", false)
        // Initializing the ViewPagerAdapter
            homeAdapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
            tab.addTab(tab.newTab().setText("Home")) // Add "Dashboard" tab second
            tab.addTab(tab.newTab().setText("Dashboard"))      // Add "Home" tab first

// Adding the Adapter to the ViewPager
        pager.adapter = homeAdapter
        if(!showDashboard && (dashboardBool == null || !dashboardBool)) {
            pager.post {
                pager.setCurrentItem(0, false)
            }
        }

        // Adding the Adapter to the ViewPager
        pager.adapter = homeAdapter
        tab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    pager.currentItem = tab.position
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tab.selectTab(tab.getTabAt(position))
            }
        })


        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        viewModel.navigateToLoginPage.observe(this) {
            if (it) {
                startActivity(Intent(this, LoginActivity::class.java))
                viewModel.navigateToLoginPageComplete()
                finish()
            }
        }


        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.abha_id_activity -> {
                    // Start the DestinationActivity
                    startActivity(Intent(this, AbhaIdActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.outreach_activity -> {
                    // Start the DestinationActivity
                    startActivity(Intent(this, OutreachActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.menu_logout -> {
                    logoutAlert.show()
                    true
                }
                else -> false
            }
        }
        // Create an ArrayList to hold your data
        val dataList = ArrayList<String>()
        dataList.add("Item 1")
        dataList.add("Item 2")
        dataList.add("Item 3")
        dataList.add("Item 3")
        dataList.add("Item 3")
        dataList.add("Item 3")
        dataList.add("Item 3")
        dataList.add("Item 3")

        // Create an ArrayAdapter to bind the data to the ListView
        var adapter = PatientListAdapter(this, benificiaryList)

        triggerAlarmManager()

        currentLanguage = prefDao.getCurrentLanguage()

    }
    private val logoutAlert by lazy {
        MaterialAlertDialogBuilder(this).setTitle(getString(R.string.logout))
            .setMessage(getString(R.string.please_confirm_to_logout))
            .setPositiveButton(getString(R.string.select_yes)) { dialog, _ ->
                viewModel.logout(myLocation,"By User")
                dialog.dismiss()
            }.setNegativeButton(getString(R.string.select_no)) { dialog, _ ->

                dialog.dismiss()
            }.create()
    }
private fun triggerAlarmManager(){
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val alarmIntent = Intent(this, AutoLogoutReceiver::class.java)
    alarmIntent.putExtra("alarmMgrLatitude", myLocation?.latitude)
    alarmIntent.putExtra("alarmMgrLongitude", myLocation?.longitude)
    alarmIntent.action = "com.yourapp.ACTION_AUTO_LOGOUT"
    val pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE)

    // Set the alarm to trigger at 5 PM
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 17) // 5 PM
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)

    // Schedule the alarm to repeat daily
    alarmManager.setRepeating(
        AlarmManager.RTC,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent
    )
}
    override fun onBackPressed() {
//        super.onBackPressed()
        if (!exitAlert.isShowing)
            exitAlert.show()
    }
    private val exitAlert by lazy {
        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.exit_application))
            .setMessage(resources.getString(R.string.do_you_want_to_exit_application))
            .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                this.finish()
            }
            .setNegativeButton(resources.getString(R.string.no)) { d, _ ->
                d.dismiss()
            }
            .create()
    }
    private fun showLanguagePopUp() {

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_radio_btns, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setTitle("Choose Application Language")

            .setPositiveButton("Apply") { dialog, which ->
                prefDao.saveSetLanguage(currentLanguage)
                Locale.setDefault(Locale(currentLanguage.symbol))

                val refresh = Intent(this, HomeActivity::class.java)
                finish()
                startActivity(refresh)
                this?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rg_lang_select_dialog)
        val englishRadioButton = dialogView.findViewById<MaterialRadioButton>(R.id.rb_eng_dialog)
        val kannadaRadioButton = dialogView.findViewById<MaterialRadioButton>(R.id.rb_kannada_dialog)
        if (radioGroup != null && englishRadioButton != null && kannadaRadioButton != null) {

            when (prefDao.getCurrentLanguage()) {
                Languages.ENGLISH -> radioGroup.check(englishRadioButton.id)
                Languages.KANNADA -> radioGroup.check(kannadaRadioButton.id)
            }

            radioGroup.setOnCheckedChangeListener { _, i ->
                currentLanguage = when (i) {
                    englishRadioButton.id -> Languages.ENGLISH
                    kannadaRadioButton.id -> Languages.KANNADA
                    else -> Languages.ENGLISH
                }
            }
        }
        dialog.show()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    private fun setUpNavHeader() {
        CoroutineScope(Dispatchers.IO).launch {
            val user = userRepo.getLoggedInUser()
            val headerView = binding.navView.getHeaderView(0)
                headerView.findViewById<TextView>(R.id.tv_nav_name).text =
                    getString(R.string.nav_item_1_text, user?.name)
                headerView.findViewById<TextView>(R.id.tv_nav_role).text =
                    getString(R.string.nav_item_2_text, user?.userName)
                headerView.findViewById<TextView>(R.id.tv_nav_id).text =
                    getString(R.string.nav_item_3_text, user?.userId)
        }

    }

    private fun getCurrentLocation() {
        // Check if location permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            //  Location listener
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    myLocation = location
                    Log.i("Location From home is", "${myLocation!!.longitude}")
                }

                override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

                override fun onProviderEnabled(provider: String) {}

                override fun onProviderDisabled(provider: String) {
                    Toast.makeText(
                        applicationContext, "Location Provider/GPS disabled",
                        Toast.LENGTH_SHORT
                    ).show()
                    val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(settingsIntent)
                }
            }

            // Request location updates
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0,
                0f,
                locationListener!!
            )
        } else {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
        private const val MIN_TIME_BETWEEN_UPDATES: Long = 1000 // 1 second
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Float = 10f // 10 meters
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as CHOApplication).activityList.remove(this)
    }


private fun calculateDelayMillis(currentHour: Int, currentMinute: Int, desiredHour: Int, desiredMinute: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, desiredHour)
    calendar.set(Calendar.MINUTE, desiredMinute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val desiredTimeMillis = calendar.timeInMillis
    val currentTimeMillis = Calendar.getInstance().timeInMillis

    if (currentTimeMillis > desiredTimeMillis) {
        // The desired time is already in the past, schedule it for the next day
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    // Calculate the delay in milliseconds
    return calendar.timeInMillis - currentTimeMillis
}

private fun myMethodToRunAtSpecificTime() {
    // Your method code here
//    viewModel.logout(myLocation,"By System")
}
}