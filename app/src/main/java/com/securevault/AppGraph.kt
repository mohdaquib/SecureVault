package com.securevault

import android.content.Context
import com.securevault.core.network.OkHttpProvider
import com.securevault.core.network.RetrofitProvider
import com.securevault.core.network.SecurityHealthChecker
import com.securevault.data.SecureVaultDataFactory
import com.securevault.domain.usecase.CreateNoteUseCase
import com.securevault.domain.usecase.GetNotesUseCase
import com.securevault.ui.MainViewModel

object AppGraph {
    fun mainViewModel(context: Context) =
        MainViewModel(
            getNotesUseCase =
                GetNotesUseCase(
                    SecureVaultDataFactory.createNotesRepository(context),
                ),
            createNoteUseCase =
                CreateNoteUseCase(
                    SecureVaultDataFactory.createNotesRepository(context),
                ),
            securityHealthChecker =
                SecurityHealthChecker(
                    RetrofitProvider(OkHttpProvider()).createHealthApi(),
                ),
        )
}
