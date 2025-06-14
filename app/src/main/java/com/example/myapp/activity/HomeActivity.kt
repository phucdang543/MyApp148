package com.example.myapp.activity

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.databinding.ActivityHomeBinding
import com.example.myapp.fragment.FavoriteFragment
import com.example.myapp.fragment.HistoryFragment
import com.example.myapp.fragment.PlaylistFragment
import com.example.myapp.fragment.SongListFragment
import com.example.myapp.manager.MusicManager
import com.example.myapp.process.RetrofitClient
import com.example.myapp.receiver.MusicControlReceiver
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var imgbtnMenu: ImageButton
    private lateinit var imgbtnSearch: ImageButton
    private lateinit var edtSearch: EditText
    private lateinit var tvPopularSongs: TextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var musicControlReceiver: MusicControlReceiver

    private var currentFragment: Fragment? = null
    private var currentFragmentTag: String = FRAGMENT_SONGS
    private var isSearchExpanded = false

    private lateinit var musicManager: MusicManager
    private lateinit var miniPlayerContainer: View
    private lateinit var imgMiniSong: ImageView
    private lateinit var tvMiniSongTitle: TextView
    private lateinit var tvMiniArtist: TextView
    private lateinit var btnMiniPlayPause: ImageButton
    private lateinit var btnMiniPrevious: ImageButton
    private lateinit var btnMiniNext: ImageButton
    private lateinit var progressMiniPlayer: android.widget.ProgressBar
    private lateinit var songChangedReceiver: BroadcastReceiver
    private lateinit var playStateChangedReceiver: BroadcastReceiver

    companion object {
        private const val FRAGMENT_SONGS = "songs"
        private const val FRAGMENT_FAVORITES = "favorites"
        private const val FRAGMENT_PLAYLISTS = "playlists"
        private const val FRAGMENT_HISTORY = "history"
    }


    private fun setupMusicControlReceiver() {
        musicControlReceiver = MusicControlReceiver()
        val filter = IntentFilter("com.example.myapp.MUSIC_CONTROL")

        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(musicControlReceiver, filter)
    }

    private fun setupSongChangedReceiver() {
        songChangedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val currentSong = musicManager.getCurrentSong()
                if (currentSong != null) {
                    updateMiniPlayerUI(currentSong)
                }
            }
        }

        val filter = IntentFilter("com.example.myapp.SONG_CHANGED")
        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(songChangedReceiver, filter)
    }

    private fun setupPlayStateChangedReceiver() {
        playStateChangedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val isPlaying = intent?.getBooleanExtra("isPlaying", false) == true
                updateMiniPlayerPlayButton(isPlaying)
            }
        }

        val filter = IntentFilter("com.example.myapp.PLAY_STATE_CHANGED")
        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(playStateChangedReceiver, filter)


    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        initViews()
        setupNavigationDrawer()
        setupBottomNavigation()
        setupSearchFunctionality()
        setupUserProfile()
        setupMusicManager()
        setupMiniPlayer()
        setupMusicControlReceiver()
        setupSongChangedReceiver()
        setupPlayStateChangedReceiver()
        if (savedInstanceState == null) {
            loadFragment(
                SongListFragment(),
                FRAGMENT_SONGS,
                getString(R.string.explore_music_title),
                R.drawable.ic_person_apple
            )
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())


            binding.layoutTopBar.setPadding(
                binding.layoutTopBar.paddingLeft,
                systemBars.top,
                binding.layoutTopBar.paddingRight,
                binding.layoutTopBar.paddingBottom
            )


            val layoutParams =
                binding.cardBottomNav.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams.bottomMargin = navigationBars.bottom + 20
            binding.cardBottomNav.layoutParams = layoutParams

            insets
        }
    }

    private fun initViews() {
        imgbtnMenu = binding.imgbtnMenu
        imgbtnSearch = binding.imgbtnSearch
        edtSearch = binding.edtSearch
        tvPopularSongs = binding.labelPopularSongs
        drawerLayout = binding.drawerLayout
        navigationView = binding.navigationView
    }

    private fun setupNavigationDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        imgbtnMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem.itemId)
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            handleNavigationItemSelected(item.itemId)
            true
        }
    }

    private fun handleNavigationItemSelected(itemId: Int) {
        when (itemId) {
            R.id.nav_songs, R.id.bottom_nav_home -> {
                if (currentFragmentTag != FRAGMENT_SONGS) {
                    loadFragment(
                        SongListFragment(),
                        FRAGMENT_SONGS,
                        getString(R.string.explore_music_title),
                        R.drawable.ic_person_apple
                    )
                    updateNavigationSelection(R.id.nav_songs, R.id.bottom_nav_home)
                }
            }

            R.id.nav_favorites, R.id.bottom_nav_favorites -> {
                if (currentFragmentTag != FRAGMENT_FAVORITES) {
                    loadFragment(
                        FavoriteFragment(),
                        FRAGMENT_FAVORITES,
                        getString(R.string.favorites_title),
                        R.drawable.ic_heart_apple
                    )
                    updateNavigationSelection(R.id.nav_favorites, R.id.bottom_nav_favorites)
                }
            }

            R.id.nav_playlists, R.id.bottom_nav_playlists -> {
                if (currentFragmentTag != FRAGMENT_PLAYLISTS) {
                    loadFragment(
                        PlaylistFragment(),
                        FRAGMENT_PLAYLISTS,
                        "Playlist",
                        R.drawable.ic_playlist_apple
                    )
                    updateNavigationSelection(R.id.nav_playlists, R.id.bottom_nav_playlists)
                }
            }

            R.id.nav_history, R.id.bottom_nav_history -> {
                if (currentFragmentTag != FRAGMENT_HISTORY) {
                    loadFragment(
                        HistoryFragment(),
                        FRAGMENT_HISTORY,
                        getString(R.string.history_title),
                        R.drawable.ic_history_apple
                    )
                    updateNavigationSelection(R.id.nav_history, R.id.bottom_nav_history)
                }
            }

            R.id.nav_logout -> {
                showLogoutConfirmation()
            }

        }
    }

    private fun loadFragment(fragment: Fragment, tag: String, title: String, icon: Int) {
        currentFragment = fragment
        currentFragmentTag = tag

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment, tag)
            commit()
        }

        tvPopularSongs.text = title
        Glide.with(this).load(icon).into(imgbtnMenu)


        updateSearchVisibility(tag)
    }

    private fun updateNavigationSelection(drawerItemId: Int, bottomItemId: Int) {
        navigationView.setCheckedItem(drawerItemId)
        binding.bottomNavigation.selectedItemId = bottomItemId
    }

    private fun updateSearchVisibility(fragmentTag: String) {
        val showSearchIcon = fragmentTag == FRAGMENT_SONGS


        binding.cardSearch.visibility =
            if (showSearchIcon) View.VISIBLE else View.GONE
        imgbtnSearch.visibility =
            if (showSearchIcon) View.VISIBLE else View.GONE


        if (!showSearchIcon && isSearchExpanded) {
            collapseSearchBar()
        }


        if (!isSearchExpanded) {
            binding.cardSearchBar.visibility = View.GONE
        }
    }

    private fun showKeyboard() {
        val inputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        inputMethodManager.showSoftInput(
            edtSearch,
            android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
        )
    }


    private fun setupSearchFunctionality() {
        imgbtnSearch.setOnClickListener {
            if (isSearchExpanded) {
                collapseSearchBar()
            } else {
                expandSearchBar()
            }
        }

        edtSearch.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }


        edtSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty() && isSearchExpanded) {

                    resetSearchResults()
                }
            }
        })


        binding.root.setOnClickListener {
            if (isSearchExpanded) {
                collapseSearchBar()
            }
        }
    }

    private fun resetSearchResults() {
        val fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_SONGS)
        if (fragment is SongListFragment) {
            fragment.clearSearch()
        }
    }

    private fun expandSearchBar() {
        isSearchExpanded = true


        binding.cardSearchBar.visibility = View.VISIBLE
        binding.cardSearchBar.alpha = 0f
        binding.cardSearchBar.animate()
            .alpha(1f)
            .setDuration(200)
            .start()


        edtSearch.requestFocus()
        showKeyboard()


        imgbtnSearch.setImageResource(R.drawable.ic_close_apple)


    }


    private fun collapseSearchBar() {
        isSearchExpanded = false


        resetSearchResults()


        binding.cardSearchBar.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.cardSearchBar.visibility = View.GONE
                edtSearch.text.clear()
            }
            .start()


        hideKeyboard()


        imgbtnSearch.setImageResource(R.drawable.ic_search_apple)


        tvPopularSongs.text = getString(R.string.explore_music_title)
    }


    private fun performSearch() {
        val query = edtSearch.text.toString().trim()
        if (query.isEmpty()) {
            showError(getString(R.string.search_empty_query))
            return
        }

        val fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_SONGS)
        if (fragment is SongListFragment) {
            fragment.performSearch(query)

            tvPopularSongs.text = getString(R.string.search_for, query)
        } else {
            val songFragment = SongListFragment()
            loadFragment(
                songFragment,
                FRAGMENT_SONGS,
                getString(R.string.search_results_title),
                R.drawable.ic_search_apple
            )
            updateNavigationSelection(R.id.nav_songs, R.id.bottom_nav_home)

            songFragment.view?.post {
                songFragment.performSearch(query)
                tvPopularSongs.text = getString(R.string.search_for, query)
            }
        }

        edtSearch.clearFocus()
        hideKeyboard()
    }

    private fun setupUserProfile() {

        lifecycleScope.launch {
            try {
                val userResponse = RetrofitClient.userService.getUserProfile()


                val headerView = navigationView.getHeaderView(0)
                val tvUserName = headerView.findViewById<TextView>(R.id.tv_user_name)

                tvUserName.text = userResponse.name

            } catch (e: Exception) {
                e.printStackTrace()

                val headerView = navigationView.getHeaderView(0)
                val tvUserName = headerView.findViewById<TextView>(R.id.tv_user_name)

                tvUserName.text = "Music Lover"
            }
        }
    }

    private fun showLogoutConfirmation() {
        showStyledDialog(R.layout.dialog_logout) { view, dialog ->
            view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
                performLogout()
            }
            view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                dialog.dismiss()
            }
        }
    }

    private fun showStyledDialog(
        layoutRes: Int,
        setupDialog: (View, AlertDialog) -> Unit
    ) {


        try {
            val dialogView = LayoutInflater.from(this@HomeActivity).inflate(layoutRes, null)

            val dialog = AlertDialog.Builder(this@HomeActivity, R.style.AppleMusicDialogTheme)
                .setView(dialogView)
                .setCancelable(true)
                .create()


            dialog.window?.let { window ->
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window.setDimAmount(0.7f)


                val params = window.attributes
                params.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                window.attributes = params


                window.attributes = window.attributes.apply {
                    windowAnimations = R.style.DialogAnimation
                }
            }


            setupDialog(dialogView, dialog)

            dialog.show()

        } catch (e: Exception) {
            e.printStackTrace()
            showError(getString(R.string.error_show_dialog))
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {

                RetrofitClient.authService.logout()
            } catch (e: Exception) {
                e.printStackTrace()

            } finally {

                clearUserData()


                val intent = Intent(this@HomeActivity, SignInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun clearUserData() {

        val authPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        authPrefs.edit().clear().apply()

        val favPrefs = getSharedPreferences("favorites", MODE_PRIVATE)
        favPrefs.edit().clear().apply()

        val historyPrefs = getSharedPreferences("playback_history", MODE_PRIVATE)
        historyPrefs.edit().clear().apply()


        musicManager.clear()
        musicManager.stopMusicService(this)
    }

    private fun showComingSoonMessage(feature: String) {
        Snackbar.make(
            binding.root,
            getString(R.string.feature_coming_soon, feature),
            Snackbar.LENGTH_SHORT
        )
            .show()
    }

    private fun showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.about_title))
            .setMessage(getString(R.string.about_message))
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    override fun onBackPressed() {
        when {
            isSearchExpanded -> {

                collapseSearchBar()
            }

            drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                drawerLayout.closeDrawer(GravityCompat.START)
            }

            currentFragmentTag != FRAGMENT_SONGS -> {
                loadFragment(
                    SongListFragment(),
                    FRAGMENT_SONGS,
                    getString(R.string.explore_music_title),
                    R.drawable.ic_person_apple
                )
                updateNavigationSelection(R.id.nav_songs, R.id.bottom_nav_home)
            }

            else -> {
                super.onBackPressed()
            }
        }
    }

    private fun setupMusicManager() {
        musicManager = MusicManager.getInstance()

        musicManager.currentSong.observe(this) { song ->
            if (song != null) {
                val isActuallyPlaying = musicManager.isPlaying.value ?: false
                if (isActuallyPlaying) {
                    showMiniPlayer()
                    updateMiniPlayerUI(song)
                }
            } else {
                hideMiniPlayer()
            }
        }
        musicManager.isPlaying.observe(this) { isPlaying ->
            updateMiniPlayerPlayButton(isPlaying)
        }


        musicManager.currentPosition.observe(this) { position ->
            updateMiniPlayerProgress(position)
        }
    }

    private fun setupMiniPlayer() {
        miniPlayerContainer = binding.miniPlayerContainer


        imgMiniSong = binding.miniPlayer.root.findViewById(R.id.img_mini_song)
        tvMiniSongTitle = binding.miniPlayer.root.findViewById(R.id.tv_mini_song_title)
        tvMiniArtist = binding.miniPlayer.root.findViewById(R.id.tv_mini_artist)
        btnMiniPlayPause = binding.miniPlayer.root.findViewById(R.id.btn_mini_play_pause)
        btnMiniPrevious = binding.miniPlayer.root.findViewById(R.id.btn_mini_previous)
        btnMiniNext = binding.miniPlayer.root.findViewById(R.id.btn_mini_next)
        progressMiniPlayer = binding.miniPlayer.root.findViewById(R.id.progress_mini_player)

        binding.miniPlayer.root.setOnClickListener {
            openPlaySongActivity()
        }

        btnMiniPlayPause.setOnClickListener {
            togglePlayPause()
        }

        btnMiniPrevious.setOnClickListener {
            previousSong()
        }

        btnMiniNext.setOnClickListener {
            nextSong()
        }
    }

    private fun showMiniPlayer() {
        if (miniPlayerContainer.visibility != View.VISIBLE) {
            miniPlayerContainer.visibility = View.VISIBLE
            miniPlayerContainer.alpha = 0f
            miniPlayerContainer.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    private fun hideMiniPlayer() {
        if (miniPlayerContainer.visibility == View.VISIBLE) {
            miniPlayerContainer.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    miniPlayerContainer.visibility = View.GONE
                }
                .start()
        }
    }

    private fun updateMiniPlayerUI(song: com.example.myapp.process.getsong.Song) {
        tvMiniSongTitle.text = song.title
        tvMiniArtist.text = song.artist.name
        Glide.with(this)
            .load(song.imageUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(imgMiniSong)
    }

    private fun updateMiniPlayerPlayButton(isPlaying: Boolean) {
        val iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        btnMiniPlayPause.setImageResource(iconRes)
    }

    private fun updateMiniPlayerProgress(position: Int) {
        val duration = musicManager.duration.value ?: 0
        if (duration > 0) {
            val progress = (position * 100) / duration
            progressMiniPlayer.progress = progress
        }
    }

    private fun openPlaySongActivity() {
        val currentSong = musicManager.getCurrentSong()
        val playlist = musicManager.getPlaylist()

        if (currentSong != null && playlist.isNotEmpty()) {
            musicManager.syncPositionBySong()
            val position = musicManager.getCurrentSongPosition()

            val actualPosition = playlist.indexOfFirst { it.id == currentSong.id }
            val finalPosition = if (actualPosition >= 0) actualPosition else position

            val intent = Intent(this, PlaySongActivity::class.java)
            intent.putParcelableArrayListExtra("playlist", playlist)
            intent.putExtra("position", finalPosition)
            startActivity(intent)
        }
    }

    private fun togglePlayPause() {
        val intent = Intent("com.example.myapp.MUSIC_CONTROL")
        intent.putExtra("action", "play_pause")

        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)
    }

    private fun previousSong() {
        val intent = Intent("com.example.myapp.MUSIC_CONTROL")
        intent.putExtra("action", "previous")

        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)
    }

    private fun nextSong() {
        val intent = Intent("com.example.myapp.MUSIC_CONTROL")
        intent.putExtra("action", "next")

        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        drawerLayout.removeDrawerListener(drawerToggle)

        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .unregisterReceiver(songChangedReceiver)

        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .unregisterReceiver(playStateChangedReceiver)
    }
}