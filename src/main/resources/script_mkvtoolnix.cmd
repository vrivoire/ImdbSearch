@echo off
chcp 65001
SETLOCAL ENABLEDELAYEDEXPANSION

set "DIRECTORY=A Shop for Killers 2024"
rem set "LANG=eng,fre"
set "LANG=eng"
rem set "LANG=fre"

set "mkvmerge=C:\Program Files\MKVToolNix\mkvmerge.exe"
set "VIDEO=C:\Users\ADELE\Videos\"
cd %VIDEO%
set "INPUT_DIR=%VIDEO%%DIRECTORY%"
set "OUTPUT_DIR=%VIDEO%%DIRECTORY%\out"
rem set "FLITER=%INPUT_DIR%\*.mkv"
set "FLITER=%INPUT_DIR%\*.m*"

for %%f in ("%FLITER%") do (
	@echo:
	@echo ----- %%f %LANG%
	call "%mkvmerge%" -o "%OUTPUT_DIR%\%%~nf_%LANG%.mkv" --audio-tracks %LANG% --subtitle-tracks %LANG% "%%f"    
)

pause