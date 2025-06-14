package com.example.myapp.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapp.R
import com.example.myapp.activity.PlaySongActivity
import com.example.myapp.adapter.PlaylistSongAdapter
import com.example.myapp.databinding.FragmentPlaylistListBinding
import com.example.myapp.process.RetrofitClient
import com.example.myapp.process.playlist.PlaylistSongItem
import com.example.myapp.process.playlist.RemoveSongFromPlaylistRequest
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PlaylistListFragment : Fragment() {

    private var _binding: FragmentPlaylistListBinding? = null
    private val binding get() = _binding!!

    private lateinit var songAdapter: PlaylistSongAdapter
    private var playlistId: Int = -1
    private var playlistName: String = ""
    private var currentPage = 1
    private val limit = 10
    private var isLoading = false
    private var hasMoreData = true


    private var loadJob: Job? = null
    private var removeJob: Job? = null

    companion object {
        private const val ARG_PLAYLIST_ID = "playlist_id"
        private const val ARG_PLAYLIST_NAME = "playlist_name"

        fun newInstance(playlistId: Int, playlistName: String): PlaylistListFragment {
            return PlaylistListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PLAYLIST_ID, playlistId)
                    putString(ARG_PLAYLIST_NAME, playlistName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlistId = it.getInt(ARG_PLAYLIST_ID)
            playlistName = it.getString(ARG_PLAYLIST_NAME, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        setupSwipeRefresh()
        loadPlaylistSongs()
    }

    private fun setupUI() {
        binding.tvPlaylistName.text = playlistName

        binding.imgbtnBack.setOnClickListener {

            if (isAdded) {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun setupRecyclerView() {
        songAdapter = PlaylistSongAdapter()

        binding.rcPlaylistSongs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = songAdapter
        }


        songAdapter.setOnItemClickListener { songItem, position ->

            if (!isAdded) return@setOnItemClickListener

            val intent = Intent(requireContext(), PlaySongActivity::class.java)
            val songList = songAdapter.currentList.map { it.song }
            intent.putParcelableArrayListExtra("playlist", ArrayList(songList))
            intent.putExtra("song", songItem.song)
            intent.putExtra("position", position)
            startActivity(intent)
        }


        songAdapter.setOnRemoveClickListener { songItem, position ->
            removeSongFromPlaylist(songItem, position)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshPlaylistSongs()
        }
    }

    private fun loadPlaylistSongs() {
        if (isLoading || playlistId == -1) return


        loadJob?.cancel()

        isLoading = true


        if (_binding != null) {
            showLoading(true)
        }

        loadJob = lifecycleScope.launch {
            try {
                val response = RetrofitClient.playlistService.getPlaylistSongs(
                    playlistId = playlistId,
                    page = currentPage,
                    limit = limit
                )


                if (!isAdded || _binding == null) return@launch

                val songs = response.data
                hasMoreData = currentPage < response.totalPages

                if (currentPage == 1) {
                    songAdapter.submitList(songs)
                } else {
                    val currentList = songAdapter.currentList.toMutableList()
                    currentList.addAll(songs)
                    songAdapter.submitList(currentList)
                }

                showEmptyState(songs.isEmpty() && currentPage == 1)

            } catch (e: Exception) {
                e.printStackTrace()

                if (isAdded && _binding != null) {
                    showError(getString(R.string.error_load_songs))
                }
            } finally {
                isLoading = false

                if (_binding != null) {
                    showLoading(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    private fun refreshPlaylistSongs() {
        currentPage = 1
        hasMoreData = true
        loadPlaylistSongs()
    }

    private fun removeSongFromPlaylist(songItem: PlaylistSongItem, position: Int) {

        removeJob?.cancel()

        removeJob = lifecycleScope.launch {
            try {
                val request = RemoveSongFromPlaylistRequest(songId = songItem.songId)
                RetrofitClient.playlistService.removeSongFromPlaylist(playlistId, request)


                if (!isAdded || _binding == null) return@launch


                val currentList = songAdapter.currentList.toMutableList()
                currentList.removeAt(position)
                songAdapter.submitList(currentList)

                showEmptyState(currentList.isEmpty())
                showSuccess(getString(R.string.removed_from_playlist))

            } catch (e: Exception) {
                e.printStackTrace()

                if (isAdded && _binding != null) {
                    showError(getString(R.string.error_remove_from_playlist))
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {

        _binding?.progressBar?.isVisible = show
    }

    private fun showEmptyState(show: Boolean) {

        _binding?.let { binding ->
            binding.layoutEmpty.isVisible = show
            binding.rcPlaylistSongs.isVisible = !show
        }
    }

    private fun showError(message: String) {

        _binding?.let { binding ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showSuccess(message: String) {

        _binding?.let { binding ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()


        loadJob?.cancel()
        removeJob?.cancel()


        loadJob = null
        removeJob = null


        _binding = null
    }
}