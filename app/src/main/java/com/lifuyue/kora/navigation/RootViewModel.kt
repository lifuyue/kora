package com.lifuyue.kora.navigation

import androidx.lifecycle.ViewModel
import com.lifuyue.kora.core.database.connection.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RootViewModel
    @Inject
    constructor(
        connectionRepository: ConnectionRepository,
    ) : ViewModel() {
        val snapshot = connectionRepository.snapshot
    }
