package com.app.cookify.core.data

import com.app.cookify.core.common.CookifyDispatcher
import com.app.cookify.core.common.Dispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
internal object DispatchersModule {

    @Provides
    @Dispatcher(CookifyDispatcher.IO)
    fun entradaSalida(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Dispatcher(CookifyDispatcher.DEFAULT)
    fun porDefecto(): CoroutineDispatcher = Dispatchers.Default
}
