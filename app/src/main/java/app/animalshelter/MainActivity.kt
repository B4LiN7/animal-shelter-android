package app.animalshelter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.FrameLayout
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

    private lateinit var apiSrv: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        apiSrv = ApiService(this)
        initViews()

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close
        )
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.white)
        drawerLayout?.addDrawerListener(toggle)
        toggle.syncState()

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
                        apiSrv.authInterface.logout()
                        Toast.makeText(this@MainActivity, "Logged out", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            true
        }
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
}