package com.example.myapp.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.databinding.DialogAddToPlaylistBinding
import com.example.myapp.databinding.ItemSelectPlaylistBinding
import com.example.myapp.process.RetrofitClient
import com.example.myapp.process.getsong.Song
import com.example.myapp.process.playlist.AddSongToPlaylistRequest
import com.example.myapp.process.playlist.PlaylistResponse
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AddToPlaylistDialogFragment : DialogFragment() {

    private var _binding: DialogAddToPlaylistBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SelectPlaylistAdapter
    private var currentUserId: Int = -1
    private var song: Song? = null
    private var onSuccess: (() -> Unit)? = null

    companion object {
        private const val ARG_SONG = "song"


        fun newInstance(song: Song, onSuccess: () -> Unit): AddToPlaylistDialogFragment {
            return AddToPlaylistDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SONG, song)
                }
                this.onSuccess = onSuccess
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        song = arguments?.getParcelable(ARG_SONG)


        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddToPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDialog()
        setupDialogWindow()
        setupRecyclerView()
        getCurrentUserIdAndLoadPlaylists()
    }

    private fun setupDialog() {
        song?.let {
            binding.tvSongName.text = it.title
            binding.tvArtistName.text = it.artist.name
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setupDialogWindow() {
        dialog?.window?.let { window ->

            window.setBackgroundDrawable(ColorDrawable(Color.parseColor("#80000000")))


            val params = window.attributes
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            window.attributes = params


            window.setDimAmount(0.7f)
        }
    }

    private fun setupRecyclerView() {
        adapter = SelectPlaylistAdapter { playlist ->
            addSongToPlaylist(playlist)
        }

        binding.rcPlaylists.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AddToPlaylistDialogFragment.adapter

            isNestedScrollingEnabled = true
        }
    }

    private fun getCurrentUserIdAndLoadPlaylists() {
        lifecycleScope.launch {
            try {

                showLoading(true)

                val userResponse = RetrofitClient.userService.getUserProfile()
                currentUserId = userResponse.id


                loadPlaylists()

            } catch (e: Exception) {
                showError(getString(R.string.error_get_user_info))
                showLoading(false)
            }
        }
    }

    private fun loadPlaylists() {
        if (currentUserId == -1) {
            return
        }

        lifecycleScope.launch {
            try {

                val response = RetrofitClient.playlistService.getUserPlaylists(currentUserId)
                val playlists = response.data



                if (playlists.isEmpty()) {
                    showEmptyState(true)
                } else {
                    showEmptyState(false)
                    adapter.submitList(playlists)
                }

            } catch (e: Exception) {
                showError(getString(R.string.error_load_playlists))
                showEmptyState(true)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun addSongToPlaylist(playlist: PlaylistResponse) {
        song?.let { currentSong ->
            lifecycleScope.launch {
                try {

                    val request = AddSongToPlaylistRequest(songId = currentSong.id)
                    RetrofitClient.playlistService.addSongToPlaylist(playlist.id, request)

                    showSuccess(getString(R.string.added_to_playlist, playlist.name))
                    onSuccess?.invoke()

                    kotlinx.coroutines.delay(1000)
                    dismiss()

                } catch (e: Exception) {
                    showError(getString(R.string.error_add_to_playlist))
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.rcPlaylists.visibility = if (show) View.GONE else View.VISIBLE
        binding.layoutEmpty?.visibility = View.GONE
    }

    private fun showEmptyState(show: Boolean) {
        binding.layoutEmpty?.visibility = if (show) View.VISIBLE else View.GONE
        binding.rcPlaylists.visibility = if (show) View.GONE else View.VISIBLE


    }

    private fun showError(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(Color.parseColor("#FF375F"))
                .setTextColor(Color.WHITE)
                .show()
        }

    }

    private fun showSuccess(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(Color.parseColor("#34C759"))
                .setTextColor(Color.WHITE)
                .show()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    inner class SelectPlaylistAdapter(
        private val onPlaylistClick: (PlaylistResponse) -> Unit
    ) : RecyclerView.Adapter<SelectPlaylistAdapter.ViewHolder>() {

        private var playlists: List<PlaylistResponse> = emptyList()

        inner class ViewHolder(val binding: ItemSelectPlaylistBinding) :
            RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onPlaylistClick(playlists[position])
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemSelectPlaylistBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val playlist = playlists[position]

            holder.binding.apply {
                tvPlaylistName.text = playlist.name
                tvCreatedDate.text = formatDate(playlist.createdAt)


                root.alpha = 0.8f
                root.animate()
                    .alpha(1.0f)
                    .setDuration(200)
                    .setStartDelay(position * 50L)
                    .start()
            }
        }

        override fun getItemCount() = playlists.size

        fun submitList(newPlaylists: List<PlaylistResponse>) {
            playlists = newPlaylists
            notifyDataSetChanged()
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    java.util.Locale.getDefault()
                )
                val outputFormat = java.text.SimpleDateFormat(
                    "dd/MM/yyyy",
                    java.util.Locale.getDefault()
                )
                val date = inputFormat.parse(dateString)
                getString(R.string.created_date, outputFormat.format(date ?: return ""))
            } catch (e: Exception) {
                getString(R.string.created_recently)
            }
        }
    }
}