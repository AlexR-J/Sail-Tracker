package diss.testing.runningapp2.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import diss.testing.runningapp2.db.SessionDatabase
import diss.testing.runningapp2.other.Constants.KEY_FIRST_TIME_TOGGLE
import diss.testing.runningapp2.other.Constants.KEY_NAME
import diss.testing.runningapp2.other.Constants.KEY_WEIGHT
import diss.testing.runningapp2.other.Constants.SESSION_DATABASE_NAME
import diss.testing.runningapp2.other.Constants.SHARAED_PREFFERENCES_NAME
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideSessionDatabase(
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(
        app,
        SessionDatabase::class.java,
        SESSION_DATABASE_NAME
    ).allowMainThreadQueries().build()

    @Singleton
    @Provides
    fun provideRunDao(db: SessionDatabase) = db.getRunDao()


    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext app : Context) =
        app.getSharedPreferences(SHARAED_PREFFERENCES_NAME, MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideName(sharePref : SharedPreferences) = sharePref.getString(KEY_NAME, "")?: ""

    @Singleton
    @Provides
    fun provideWeight(sharePref : SharedPreferences) = sharePref.getFloat(KEY_WEIGHT, 80f)

    @Singleton
    @Provides
    fun provideFirstTimeToggle(sharePref : SharedPreferences) = sharePref.getBoolean(KEY_FIRST_TIME_TOGGLE, true)


}