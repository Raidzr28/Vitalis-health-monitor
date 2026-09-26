package com.vitalis.core.database.di

import android.content.Context
import androidx.room.Room
import com.vitalis.core.database.VitalisDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // ponytail: plain SQLite for now; spec §6.1 wants SQLCipher — add the openHelperFactory here when auth lands.
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VitalisDatabase =
        Room.databaseBuilder(context, VitalisDatabase::class.java, "vitalis.db").build()
}
