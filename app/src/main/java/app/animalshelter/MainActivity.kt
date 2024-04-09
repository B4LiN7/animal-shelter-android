package app.animalshelter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.DatePicker
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import app.animalshelter.api.ApiService
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Views
    private var toolbar: Toolbar? = null
    private var drawerLayout: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var frameLayout: FrameLayout? = null

    // API Service
    private lateinit var apiSrv: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apiSrv = ApiService(this)
        setContentView(R.layout.activity_main)
        initViews()
        initHeader()

        lifecycleScope.launch {
            val reachable = apiSrv.apiTest()
            if (!reachable) {
                Toast.makeText(this@MainActivity, "Nem sikerült kapcsolódni a szerverhez!", Toast.LENGTH_LONG).show()

                var logoutItem = navigationView?.menu?.findItem(R.id.nav_logout)
                logoutItem?.isVisible = false
                var loginItem = navigationView?.menu?.findItem(R.id.nav_login)
                loginItem?.isVisible = false
                var adoptionsItem = navigationView?.menu?.findItem(R.id.nav_adoptions)
                adoptionsItem?.isVisible = false
                var petsItem = navigationView?.menu?.findItem(R.id.nav_pets)
                petsItem?.isVisible = false
                var breedsItem = navigationView?.menu?.findItem(R.id.nav_breeds)
                breedsItem?.isVisible = false

                val fragment = SettingsFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.Main_FrameLayout, fragment)
                    .commit()
            }
        }

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close
        )
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.white)
        drawerLayout?.addDrawerListener(toggle)
        toggle.syncState()

        // Navigation between fragments
        navigationView?.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_login -> {
                    val fragment = LoginFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.Main_FrameLayout, fragment)
                        .commit()
                }

                R.id.nav_adoptions -> {
                    val fragment = AdoptionsFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.Main_FrameLayout, fragment)
                        .commit()
                }

                R.id.nav_pets -> {
                    val fragment = PetsFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.Main_FrameLayout, fragment)
                        .commit()
                }

                R.id.nav_breeds -> {
                    val fragment = BreedsFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.Main_FrameLayout, fragment)
                        .commit()
                }

                R.id.nav_settings -> {
                    val fragment = SettingsFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.Main_FrameLayout, fragment)
                        .commit()
                }

                R.id.nav_logout -> {
                    lifecycleScope.launch {
                        apiSrv.logout()
                        initHeader()
                    }
                }
            }
            true
        }

    }

    private fun initViews() {
        toolbar = findViewById(R.id.Main_Toolbar)
        drawerLayout = findViewById(R.id.Main_DrawerLayout)
        navigationView = findViewById(R.id.Main_NavigationView)
        frameLayout = findViewById(R.id.Main_FrameLayout)
    }
    private fun initHeader() {
        val headerView = navigationView?.getHeaderView(0)
        val textView = headerView?.findViewById<TextView>(R.id.Header_TextView)
        lifecycleScope.launch {
            val user = apiSrv.fetchCurrentUser()
            if (user != null) {
                textView?.text = "Üdvözöljük, ${user.name}!"
                var logoutItem = navigationView?.menu?.findItem(R.id.nav_logout)
                logoutItem?.isVisible = true
                var loginItem = navigationView?.menu?.findItem(R.id.nav_login)
                loginItem?.isVisible = false

                val fragment = WelcomeFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.Main_FrameLayout, fragment)
                    .commit()
            } else {
                textView?.text = "Üdvözöljük!"
                var item = navigationView?.menu?.findItem(R.id.nav_logout)
                item?.isVisible = false
                var loginItem = navigationView?.menu?.findItem(R.id.nav_login)
                loginItem?.isVisible = true

                val fragment = LoginFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.Main_FrameLayout, fragment)
                    .commit()
            }
        }
    }
}