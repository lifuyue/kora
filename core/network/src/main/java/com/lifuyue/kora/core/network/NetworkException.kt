package com.lifuyue.kora.core.network

import com.lifuyue.kora.core.common.NetworkError
import java.io.IOException

class NetworkException(
    val networkError: NetworkError,
) : IOException(networkError.message)
