package com.example.tunespipe

import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Provides a single, shared instance of OkHttpClient for the entire app.
 * This is efficient as it allows for connection pooling and a single point of configuration.
 *
 * It is configured to:
 * - Force IPv4 connections to avoid issues on some networks.
 * - Set a reasonable read timeout.
 */
object HttpClient {
    val instance: OkHttpClient by lazy {
        // A DNS resolver that filters for IPv4 addresses only.
        val ipv4Dns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                return Dns.SYSTEM.lookup(hostname).filter { Inet4Address::class.java.isInstance(it) }
            }
        }

        OkHttpClient.Builder()
            .dns(ipv4Dns)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
