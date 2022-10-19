package com.ojhdtapp.parabox.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.Converters
import com.ojhdtapp.parabox.data.repository.MainRepositoryImpl
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.use_case.*
import com.ojhdtapp.parabox.domain.util.GsonParser
import com.ojhdtapp.parabox.domain.worker.UploadFileWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase =
        Room.databaseBuilder(
            app, AppDatabase::class.java,
            "main_db"
        )
            .addTypeConverter(Converters(GsonParser()))
            .build()

    @Provides
    @Singleton
    fun provideMainRepository(
        database: AppDatabase,
        @ApplicationContext applicationContext: Context,
        notificationUtil: NotificationUtil
    ): MainRepository =
        MainRepositoryImpl(database, applicationContext, notificationUtil)

    @Provides
    @Singleton
    fun provideNotificationUtil(
        @ApplicationContext applicationContext: Context,
        database: AppDatabase,
    ): NotificationUtil =
        NotificationUtil(applicationContext, database)

    @Provides
    @Singleton
    fun provideHandleNewMessageUseCase(repository: MainRepository): HandleNewMessage {
        return HandleNewMessage(repository)
    }

    @Provides
    @Singleton
    fun provideGetContactListUseCase(repository: MainRepository): GetContacts {
        return GetContacts(repository)
    }

    @Provides
    @Singleton
    fun provideGetGroupInfoPackUseCase(repository: MainRepository): GetGroupInfoPack {
        return GetGroupInfoPack(repository)
    }

    @Provides
    @Singleton
    fun provideGroupNewContactUseCase(repository: MainRepository): GroupNewContact {
        return GroupNewContact(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateContactUseCase(repository: MainRepository): UpdateContact {
        return UpdateContact(repository)
    }

    @Provides
    @Singleton
    fun provideTagControlUseCase(repository: MainRepository): TagControl {
        return TagControl(repository)
    }

    @Provides
    @Singleton
    fun provideGetArchivedContactsUseCase(repository: MainRepository): GetArchivedContacts {
        return GetArchivedContacts(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateMessageUseCase(repository: MainRepository): UpdateMessage {
        return UpdateMessage(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteGroupedContactUseCase(repository: MainRepository): DeleteGroupedContact {
        return DeleteGroupedContact(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteMessageUseCase(repository: MainRepository): DeleteMessage {
        return DeleteMessage(repository)
    }

    @Provides
    @Singleton
    fun provideGetFilesUseCase(repository: MainRepository): GetFiles {
        return GetFiles(repository)
    }

    @Provides
    @Singleton
    fun provideQueryContactAndMessageUseCase(repository: MainRepository): QueryContactAndMessage {
        return QueryContactAndMessage(repository)
    }
}