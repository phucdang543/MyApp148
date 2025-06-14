package com.example.myapp.fragment

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapp.R
import com.example.myapp.adapter.PlaylistAdapter
import com.example.myapp.databinding.FragmentPlaylistBinding
import com.example.myapp.process.RetrofitClient
import com.example.myapp.process.playlist.CreatePlaylistRequest
import com.example.myapp.process.playlist.PlaylistResponse
import com.example.myapp.process.playlist.UpdatePlaylistRequest
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PlaylistFragment : Fragment() {

    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

    private lateinit var playlistAdapter: PlaylistAdapter
    private var currentUserId: Int = -1

    private var loadJob: Job? = null
    private var userJob: Job? = null
    private var createJob: Job? = null
    private var updateJob: Job? = null
    private var deleteJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getCurrentUserId()
        setupRecyclerView()
        setupFab()
        setupSwipeRefresh()
    }

    private fun getCurrentUserId() {
        userJob?.cancel()

        userJob = lifecycleScope.launch {
            try {
                val userResponse = RetrofitClient.userService.getUserProfile()
                if (!isAdded || _binding == null) return@launch

                currentUserId = userResponse.id
                loadPlaylists()
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded && _binding != null) {
                    showError(getString(R.string.error_get_user_info))
                }
            }
        }
    }

    private fun setupRecyclerView() {
        playlistAdapter = PlaylistAdapter()

        binding.rcPlaylists.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = playlistAdapter
        }


        playlistAdapter.setOnItemClickListener { playlist, _ ->
            openPlaylistDetail(playlist)
        }


        playlistAdapter.setOnEditClickListener { playlist, position ->
            showEditPlaylistDialog(playlist, position)
        }


        playlistAdapter.setOnDeleteClickListener { playlist, position ->
            showDeleteConfirmDialog(playlist, position)
        }
    }

    private fun setupFab() {
        binding.fabCreatePlaylist.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadPlaylists()
        }
    }

    private fun loadPlaylists() {
        if (currentUserId == -1) return


        loadJob?.cancel()


        if (_binding != null) {
            showLoading(true)
        }

        loadJob = lifecycleScope.launch {
            try {
                val response = RetrofitClient.playlistService.getUserPlaylists(currentUserId)


                if (!isAdded || _binding == null) return@launch

                val playlists = response.data

                playlistAdapter.submitList(playlists)
                showEmptyState(playlists.isEmpty())

            } catch (e: Exception) {
                e.printStackTrace()

                if (isAdded && _binding != null) {
                    showError(getString(R.string.error_load_playlists))
                }
            } finally {

                if (_binding != null) {
                    showLoading(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }


    private fun showStyledDialog(
        layoutRes: Int,
        setupDialog: (View, AlertDialog) -> Unit
    ) {
        if (!isAdded) return

        try {
            val dialogView = LayoutInflater.from(requireContext()).inflate(layoutRes, null)

            val dialog = AlertDialog.Builder(requireContext(), R.style.AppleMusicDialogTheme)
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


    private fun showCreatePlaylistDialog() {
        showStyledDialog(R.layout.dialog_create_playlist) { view, dialog ->
            val editText = view.findViewById<TextInputEditText>(R.id.edtPlaylistName)


            view.findViewById<Button>(R.id.btnCreate).setOnClickListener {
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    createPlaylist(name)
                    dialog.dismiss()
                } else {
                    editText.error = getString(R.string.enter_playlist_name)
                    editText.requestFocus()
                }
            }


            view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                dialog.dismiss()
            }


            editText.requestFocus()
            editText.postDelayed({
                showKeyboard(editText)
            }, 100)
        }
    }


    private fun showEditPlaylistDialog(playlist: PlaylistResponse, position: Int) {
        showStyledDialog(R.layout.dialog_edit_playlist) { view, dialog ->
            val editText = view.findViewById<TextInputEditText>(R.id.edtPlaylistName)
            editText.setText(playlist.name)
            editText.selectAll()


            view.findViewById<Button>(R.id.btnSave).setOnClickListener {
                val newName = editText.text.toString().trim()
                when {
                    newName.isEmpty() -> {
                        editText.error = getString(R.string.enter_playlist_name)
                        editText.requestFocus()
                    }

                    newName != playlist.name -> {
                        updatePlaylist(playlist.id, newName, position)
                        dialog.dismiss()
                    }

                    else -> dialog.dismiss()
                }
            }


            view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                dialog.dismiss()
            }


            editText.requestFocus()
            editText.postDelayed({
                showKeyboard(editText)
            }, 100)
        }
    }


    private fun showDeleteConfirmDialog(playlist: PlaylistResponse, position: Int) {
        showStyledDialog(R.layout.dialog_delete_confirm) { view, dialog ->

            val messageTextView = view.findViewById<TextView>(R.id.tvMessage)
            messageTextView.text = getString(R.string.delete_playlist_message, playlist.name)


            view.findViewById<Button>(R.id.btnDelete).setOnClickListener {
                try {
                    deletePlaylist(playlist.id, position)
                    dialog.dismiss()
                    showSuccess(getString(R.string.playlist_deleted, playlist.name))
                } catch (e: Exception) {
                    e.printStackTrace()
                    showError(getString(R.string.error_delete_playlist))
                }
            }


            view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                dialog.dismiss()
            }


            dialog.setOnDismissListener {

            }

            dialog.setOnCancelListener {

            }
        }
    }


    private fun showSimpleDeleteDialog(playlist: PlaylistResponse, position: Int) {
        if (!isAdded) return

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_playlist_title))
            .setMessage(getString(R.string.confirm_delete_playlist, playlist.name))
            .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                deletePlaylist(playlist.id, position)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }


    private fun createPlaylist(name: String) {
        createJob?.cancel()

        createJob = lifecycleScope.launch {
            try {
                val request = CreatePlaylistRequest(name = name)
                val response = RetrofitClient.playlistService.createPlaylist(request)

                if (!isAdded || _binding == null) return@launch


                val currentList = playlistAdapter.currentList.toMutableList()
                currentList.add(0, response)
                playlistAdapter.submitList(currentList)

                showEmptyState(false)
                showSuccess(getString(R.string.playlist_created_success))

            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded && _binding != null) {
                    showError(getString(R.string.error_create_playlist))
                }
            }
        }
    }

    private fun updatePlaylist(playlistId: Int, newName: String, position: Int) {
        updateJob?.cancel()

        updateJob = lifecycleScope.launch {
            try {
                val request = UpdatePlaylistRequest(name = newName)
                val response = RetrofitClient.playlistService.updatePlaylist(playlistId, request)

                if (!isAdded || _binding == null) return@launch


                val currentList = playlistAdapter.currentList.toMutableList()
                currentList[position] = response
                playlistAdapter.submitList(currentList)

                showSuccess(getString(R.string.playlist_updated_success))

            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded && _binding != null) {
                    showError(getString(R.string.error_update_playlist))
                }
            }
        }
    }

    private fun deletePlaylist(playlistId: Int, position: Int) {
        deleteJob?.cancel()

        deleteJob = lifecycleScope.launch {
            try {
                RetrofitClient.playlistService.deletePlaylist(playlistId)

                if (!isAdded || _binding == null) return@launch


                val currentList = playlistAdapter.currentList.toMutableList()
                currentList[position].name
                currentList.removeAt(position)
                playlistAdapter.submitList(currentList)

                showEmptyState(currentList.isEmpty())


            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded && _binding != null) {
                    showError(getString(R.string.error_delete_playlist))
                }
            }
        }
    }

    private fun openPlaylistDetail(playlist: PlaylistResponse) {
        if (!isAdded) return


        val fragment = PlaylistListFragment.newInstance(playlist.id, playlist.name)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }


    private fun showKeyboard(editText: EditText) {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun showLoading(show: Boolean) {
        _binding?.progressBar?.isVisible = show
    }

    private fun showEmptyState(show: Boolean) {
        _binding?.let { binding ->
            binding.layoutEmpty.isVisible = show
            binding.rcPlaylists.isVisible = !show
        }
    }

    private fun showError(message: String) {
        _binding?.let { binding ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(Color.parseColor("#FF375F"))
                .setTextColor(Color.WHITE)
                .show()
        }
    }

    private fun showSuccess(message: String) {
        _binding?.let { binding ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(Color.parseColor("#34C759"))
                .setTextColor(Color.WHITE)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()


        loadJob?.cancel()
        userJob?.cancel()
        createJob?.cancel()
        updateJob?.cancel()
        deleteJob?.cancel()


        loadJob = null
        userJob = null
        createJob = null
        updateJob = null
        deleteJob = null


        _binding = null
    }
}