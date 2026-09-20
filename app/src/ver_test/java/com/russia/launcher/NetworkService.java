package com.russia.launcher;

import com.russia.launcher.async.dto.response.GameFileInfoDto;
import com.russia.launcher.async.dto.response.LatestVersionInfoDto;
import com.russia.launcher.async.dto.response.LoaderSliderInfoResponseDto;
import com.russia.launcher.async.dto.response.MonitoringData;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

public interface NetworkService {

    String FILES_BASE_ADR = "https://files.liverussia.online/gamecache_test/";
    String FILE_INFO_URL = "https://raw.githubusercontent.com/Ocasa1050/sasamp-game-cache/main/test/files.json";
    String APK_URL = "https://files.liverussia.online/apk/test/app-ver_test-release.apk";

    @Headers("Content-Type: application/json")
    @GET("https://raw.githubusercontent.com/Ocasa1050/sasamp/fix/github-server-list/old_data.json")
    Call<MonitoringData> getMonitoringData();

    @Headers("Content-Type: application/json")
    @GET(NetworkService.FILE_INFO_URL)
    Call<GameFileInfoDto> getFilesList();

    @Headers("Content-Type: application/json")
    @GET("https://files.liverussia.online/loader_slider/texts.json")
    Call<LoaderSliderInfoResponseDto> getLoaderSliderInfo();

    @Headers("Content-Type: application/json")
    @GET("https://files.liverussia.online/apk/test/apk_info.php")
    Call<LatestVersionInfoDto> getLatestVersionInfoDto();
}
