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
import com.example.myapp.adapter.HistoryAdapter
import com.example.myapp.databinding.FragmentHistoryBinding
import com.example.myapp.process.RetrofitClient
import com.example.myapp.process.history.HistoryItem
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyAdapter: HistoryAdapter
    private var currentPage = 1
    private val limit = 20
    private var isLoading = false
    private var hasMoreData = true
    private val allHistoryItems = mutableListOf<HistoryItem>()


    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        loadHistory()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(requireContext())

        binding.rcHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter


            addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
                            loadMoreHistory()
                        }
                    }
                }
            })
        }


        historyAdapter.setOnPlayClickListener { historyItem, position ->
            playFromHistory(historyItem, position)
        }


        historyAdapter.setOnItemClickListener { historyItem, position ->
            playFromHistory(historyItem, position)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshHistory()
        }
    }

    private fun loadHistory() {
        if (isLoading) return


        loadJob?.cancel()

        isLoading = true


        if (_binding != null) {
            showLoading(currentPage == 1)
        }

        loadJob = lifecycleScope.launch {
            try {
                val response = RetrofitClient.historyService.getPlaybackHistory(
                    page = currentPage,
                    limit = limit
                )


                if (!isAdded || _binding == null) return@launch

                val historyItems = response.data
                hasMoreData = currentPage < response.totalPages

                if (currentPage == 1) {
                    allHistoryItems.clear()
                    allHistoryItems.addAll(historyItems)
                    historyAdapter.submitList(allHistoryItems.toList())
                } else {
                    allHistoryItems.addAll(historyItems)
                    historyAdapter.submitList(allHistoryItems.toList())
                }

                showEmptyState(allHistoryItems.isEmpty())

            } catch (e: Exception) {
                e.printStackTrace()

                if (isAdded && _binding != null) {
                    showError(getString(R.string.error_load_history))
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

    private fun loadMoreHistory() {
        if (!hasMoreData || isLoading) return

        currentPage++
        loadHistory()
    }

    private fun refreshHistory() {
        currentPage = 1
        hasMoreData = true
        loadHistory()
    }

    private fun playFromHistory(historyItem: HistoryItem, position: Int) {

        if (!isAdded) return


        val playlist = allHistoryItems.take(50).map { it.song }
        val songPosition = playlist.indexOfFirst { it.id == historyItem.song.id }

        val intent = Intent(requireContext(), PlaySongActivity::class.java)
        intent.putParcelableArrayListExtra("playlist", ArrayList(playlist))
        intent.putExtra("song", historyItem.song)
        intent.putExtra("position", if (songPosition >= 0) songPosition else 0)
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {

        _binding?.progressBar?.isVisible = show && currentPage == 1
    }

    private fun showEmptyState(show: Boolean) {

        _binding?.let { binding ->
            binding.layoutEmpty.isVisible = show
            binding.rcHistory.isVisible = !show
        }
    }

    private fun showError(message: String) {

        _binding?.let { binding ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()

        if (isAdded && _binding != null && allHistoryItems.isNotEmpty()) {
            refreshHistory()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()


        loadJob?.cancel()
        loadJob = null


        _binding = null
    }
}