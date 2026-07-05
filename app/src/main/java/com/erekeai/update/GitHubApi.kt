package com.erekeai.update

import retrofit2.http.GET

interface GitHubApi {

    @GET("repos/USERNAME/REPOSITORY/releases/latest")
    suspend fun latestRelease(): GitHubRelease

}
