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
import com.example.myapp.adapter.FavoriteAdapter
import com.example.myapp.databinding.FragmentFavoriteBinding
import com.example.myapp.process.RetrofitClient
import com.example.myapp.process.favorite.FavoriteRequest
import com.example.myapp.process.getsong.Song
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FavoriteFragment : Fragment() {

    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!

    private lateinit var favoriteAdapter: FavoriteAdapter
    private var currentPage = 1
    private val limit = 10
    private var isLoading = false
    private var hasMoreData = true


    private var loadJob: Job? = null
    private var removeJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        loadFavorites()
    }

    private fun setupRecyclerView() {
        favoriteAdapter = FavoriteAdapter()

        binding.rcFavorites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoriteAdapter
        }

        favoriteAdapter.setOnItemClickListener { song, position ->

            if (!isAdded) return@setOnItemClickListener

            val intent = Intent(requireContext(), PlaySongActivity::class.java)
            intent.putParcelableArrayListExtra("playlist", ArrayList(favoriteAdapter.currentList))
            intent.putExtra("song", song)
            intent.putExtra("position", position)
            startActivity(intent)
        }

        favoriteAdapter.setOnRemoveClickListener { song, position ->
            removeFromFavorites(song, position)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshFavorites()
        }
    }

    private fun loadFavorites() {
        if (isLoading) return


        loadJob?.cancel()

        isLoading = true


        if (_binding != null) {
            showLoading(true)
        }

        loadJob = lifecycleScope.launch {
            try {
                val response = RetrofitClient.favoriteService.getFavorites(
                    page = currentPage,
                    limit = limit
                )


                if (!isAdded || _binding == null) return@launch

                val songs = response.data
                hasMoreData = currentPage < response.totalPages

                if (currentPage == 1) {
                    favoriteAdapter.submitList(songs)
                } else {
                    val currentList = favoriteAdapter.currentList.toMutableList()
                    currentList.addAll(songs)
                    favoriteAdapter.submitList(currentList)
                }

                showEmptyState(songs.isEmpty() && currentPage == 1)

            } catch (e: Exception) {
                e.printStackTrace()

                if (isAdded && _binding != null) {
                    showError(getString(R.string.error_load_favorites))
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

    private fun refreshFavorites() {
        currentPage = 1
        hasMoreData = true
        loadFavorites()
    }

    private fun removeFromFavorites(song: Song, position: Int) {

        removeJob?.cancel()

        removeJob = lifecycleScope.launch {
            try {
                val request = FavoriteRequest(songId = song.id)
                RetrofitClient.favoriteService.removeFromFavorites(request)


                if (!isAdded || _binding == null) return@launch

                val currentList = favoriteAdapter.currentList.toMutableList()
                currentList.removeAt(position)
                favoriteAdapter.submitList(currentList)

                showEmptyState(currentList.isEmpty())

                _binding?.let { binding ->
                    Snackbar.make(
                        binding.root,
                        getString(R.string.removed_from_favorites),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()

                if (isAdded && _binding != null) {
                    showError(getString(R.string.error_remove_favorites))
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
            binding.rcFavorites.isVisible = !show
        }
    }

    private fun showError(message: String) {

        _binding?.let { binding ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
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