rem @echo off

set "DIRECTORY=Furies 2024"
rem set "LANG=eng,fre"
rem set "LANG=eng"
set "LANG=fre"

set "mkvmerge=C:\Program Files\MKVToolNix\mkvmerge.exe"
set "VIDEO=C:\Users\ADELE\Videos\"
cd %VIDEO%
set "INPUT_DIR=%VIDEO%%DIRECTORY%"
set "OUTPUT_DIR=%VIDEO%%DIRECTORY%\out"

for %%f in ("%INPUT_DIR%\*.mkv") do (
	@echo .
	@echo ----- %%f %LANG%
	call "%mkvmerge%" -o "%OUTPUT_DIR%\%%~nf_%LANG%.mkv" --audio-tracks %LANG% --subtitle-tracks %LANG% "%%f"
)

pause