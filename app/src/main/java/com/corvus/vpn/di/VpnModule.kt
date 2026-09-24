package com.corvus.vpn.di

import com.corvus.vpn.vpn.engines.OpenVpnEngine
import com.corvus.vpn.vpn.engines.VpnEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VpnModule {

    @Binds
    @Singleton
    abstract fun bindVpnEngine(openVpnEngine: OpenVpnEngine): VpnEngine
}
