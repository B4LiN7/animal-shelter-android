package app.animalshelter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import app.animalshelter.ApiService.RetrofitService
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private var Toolbar: Toolbar? = null
    private var DrawerLayout: DrawerLayout? = null
    private var NavigationView: NavigationView? = null
    private var FrameLayout: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        RetrofitService.initialize(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()

        val toggle = ActionBarDrawerToggle(
            this, DrawerLayout, Toolbar, R.string.open, R.string.close
        )
        toggle.drawerArrowDrawable.color = resources.getColor(R.color.white)
        DrawerLayout?.addDrawerListener(toggle)
        toggle.syncState()

        NavigationView?.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_login -> {
                    val fragment = LoginFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.Main_FrameLayout, fragment)
                        .commit()
                }

                R.id.nav_adoptions -> {
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
                    RetrofitService.cookieJar.clear()
                    RetrofitService.cookieJar.printCookiesToLog()
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
        val fragment = LoginFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.Main_FrameLayout, fragment)
            .commit()
    }

    private fun init() {
        Toolbar = findViewById(R.id.Main_Toolbar)
        DrawerLayout = findViewById(R.id.Main_DrawerLayout)
        NavigationView = findViewById(R.id.Main_NavigationView)
        FrameLayout = findViewById(R.id.Main_FrameLayout)
    }
}