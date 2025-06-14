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
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.activity.PlaySongActivity
import com.example.myapp.databinding.FragmentSongListBinding
import com.example.myapp.process.RetrofitClient
import com.example.myapp.process.getsong.Song
import com.example.myapp.process.getsong.SongAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SongListFragment : Fragment() {
    private var _binding: FragmentSongListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SongAdapter

    private var currentPage = 1
    private val limit = 20
    private var isLoading = false
    private var hasMoreData = true
    private var currentQuery: String? = null
    private val allSongs = mutableListOf<Song>()

    private var fetchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        fetchSongs()
    }

    private fun setupRecyclerView() {
        adapter = SongAdapter()
        binding.rcPopular.layoutManager = LinearLayoutManager(requireContext())
        binding.rcPopular.adapter = adapter

        adapter.setOnItemClickListener { song, position ->
            val intent = Intent(requireContext(), PlaySongActivity::class.java)
            intent.putParcelableArrayListExtra("playlist", ArrayList(adapter.currentList))
            intent.putExtra("song", song)
            intent.putExtra("position", position)
            startActivity(intent)
        }

        binding.rcPopular.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && hasMoreData) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                    ) {
                        loadMoreSongs()
                    }
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshSongs()
        }
    }

    private fun fetchSongs() {
        if (isLoading) return

        fetchJob?.cancel()
        isLoading = true

        if (_binding != null) {
            showLoading(currentPage == 1)
        }

        fetchJob = lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getSongs(
                    query = currentQuery,
                    page = currentPage,
                    limit = limit
                )

                if (!isAdded || _binding == null) return@launch

                val songs = response.data
                hasMoreData = currentPage < response.totalPages

                if (currentPage == 1) {
                    allSongs.clear()
                    allSongs.addAll(songs)
                    adapter.submitList(allSongs.toList())
                } else {
                    allSongs.addAll(songs)
                    adapter.submitList(allSongs.toList())
                }

                showEmptyState(allSongs.isEmpty())
                updateResultsInfo()

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

    private fun loadMoreSongs() {
        if (!hasMoreData || isLoading) return

        currentPage++
        fetchSongs()
    }

    private fun refreshSongs() {
        currentPage = 1
        hasMoreData = true
        fetchSongs()
    }

    fun performSearch(query: String) {
        if (!isAdded || _binding == null) return

        currentQuery = if (query.isBlank()) null else query
        currentPage = 1
        hasMoreData = true
        fetchSongs()
    }

    fun clearSearch() {
        if (!isAdded || _binding == null) return

        currentQuery = null
        currentPage = 1
        hasMoreData = true
        fetchSongs()
    }

    private fun updateResultsInfo() {
        _binding?.let { binding ->
            binding.tvResultsInfo?.let { textView ->
                when {
                    currentQuery != null -> {
                        textView.text = getString(R.string.search_results_for, currentQuery)
                        textView.isVisible = true
                    }

                    allSongs.isNotEmpty() -> {
                        textView.text = getString(R.string.all_songs_count, allSongs.size)
                        textView.isVisible = true
                    }

                    else -> {
                        textView.isVisible = false
                    }
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        _binding?.progressBar?.isVisible = show
    }

    private fun showEmptyState(show: Boolean) {
        _binding?.let { binding ->
            binding.layoutEmpty?.isVisible = show
            binding.rcPopular.isVisible = !show
        }
    }

    private fun showError(message: String) {
        _binding?.let { binding ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        fetchJob?.cancel()
        fetchJob = null

        _binding = null
    }
}