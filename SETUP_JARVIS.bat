@echo off
setlocal
 title JARVIS Android Setup
 color 0B
 echo.
 echo  ==========================================
 echo       JARVIS ANDROID - SETUP LAUNCHER
 echo  ==========================================
 echo.
 echo This launcher opens the Android project in Android Studio.
 echo It does not install an APK by itself.
 echo.
 set "PROJECT_DIR=%~dp0"
 set "STUDIO_EXE="
 if defined ANDROID_STUDIO set "STUDIO_EXE=%ANDROID_STUDIO%"
 if not defined STUDIO_EXE if exist "%ProgramFiles%\Android\Android Studio\bin\studio64.exe" set "STUDIO_EXE=%ProgramFiles%\Android\Android Studio\bin\studio64.exe"
 if not defined STUDIO_EXE if exist "%LOCALAPPDATA%\Programs\Android Studio\bin\studio64.exe" set "STUDIO_EXE=%LOCALAPPDATA%\Programs\Android Studio\bin\studio64.exe"
 if not defined STUDIO_EXE if exist "%ProgramFiles(x86)%\Android\Android Studio\bin\studio64.exe" set "STUDIO_EXE=%ProgramFiles(x86)%\Android\Android Studio\bin\studio64.exe"
 if not defined STUDIO_EXE (
   echo Android Studio was not found in the usual locations.
   echo.
   echo Install Android Studio from https://developer.android.com/studio
   echo Then open this folder: %PROJECT_DIR%
   echo.
   start "" "https://developer.android.com/studio"
   pause
   exit /b 1
 )
 echo Found Android Studio:
 echo %STUDIO_EXE%
 echo Opening JARVIS project...
 start "" "%STUDIO_EXE%" "%PROJECT_DIR%"
 echo.
 echo In Android Studio:
 echo  1. Let project sync finish and install SDK 35 if prompted.
 echo  2. Connect an Android phone with USB debugging enabled, or start an emulator.
 echo  3. Press the green Run button and choose the app configuration.
 echo.
 echo Note: JARVIS currently needs Android Studio/Gradle to build. No APK is bundled.
 pause
endlocal
