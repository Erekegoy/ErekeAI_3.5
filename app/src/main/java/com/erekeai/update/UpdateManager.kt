package com.erekeai.update

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(

    private val api: GitHubApi

) {

    suspend fun check(): GitHubRelease? {

        return try {

            api.latestRelease()

        } catch (e: Exception) {

            null

        }

    }

}
