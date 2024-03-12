package app.animalshelter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
        initHeaderText()

        lifecycleScope.launch {
            val reachable = apiSrv.apiTest()
            if (!reachable) {
                Toast.makeText(this@MainActivity, "Nem sikerült kapcsolódni a szerverhez!", Toast.LENGTH_LONG).show()
                finish()
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

                R.id.nav_logout -> {
                    lifecycleScope.launch {
                        apiSrv.logout()
                        initHeaderText()
                    }
                }
            }
            true
        }

        // Default fragment
        val fragment = LoginFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.Main_FrameLayout, fragment)
            .commit()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.Main_Toolbar)
        drawerLayout = findViewById(R.id.Main_DrawerLayout)
        navigationView = findViewById(R.id.Main_NavigationView)
        frameLayout = findViewById(R.id.Main_FrameLayout)
    }
    private fun initHeaderText() {
        val headerView = navigationView?.getHeaderView(0)
        val textView = headerView?.findViewById<TextView>(R.id.Header_TextView)
        lifecycleScope.launch {
            val user = apiSrv.fetchCurrentUser()
            if (user != null) {
                textView?.text = "Üdvözöljük, ${user.name}!"
                var item = navigationView?.menu?.findItem(R.id.nav_logout)
                item?.isVisible = true
            } else {
                textView?.text = "Üdvözöljük!"
                var item = navigationView?.menu?.findItem(R.id.nav_logout)
                item?.isVisible = false
            }
        }
    }
}