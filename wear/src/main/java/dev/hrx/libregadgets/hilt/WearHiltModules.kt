package dev.hrx.libregadgets.hilt

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.hrx.libregadgets.core.interfaces.MeasurementListener
import dev.hrx.libregadgets.core.interfaces.NotificationHelper
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WearHiltNotificationHelperModule {
    @Singleton
    @Provides
    fun provideNotificationHelper(): NotificationHelper {
        return WearNotificationHelper()
    }
}
@Module
@InstallIn(SingletonComponent::class)
object WearHiltMeasurementListenerModule {
    @Singleton
    @Provides
    fun provideMeasurementListener(): MeasurementListener {
        return WearMeasurementListener()
    }
}
