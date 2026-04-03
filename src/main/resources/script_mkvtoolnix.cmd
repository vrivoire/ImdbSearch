rem @echo off

set "DIRECTORY=Chocolat Amer 2024"
set "LANG=eng,fre"
rem set "LANG=eng"
rem set "LANG=fre"

set "mkvmerge=C:\Program Files\MKVToolNix\mkvmerge.exe"
set "VIDEO=C:\Users\ADELE\Videos\W3\"
cd %VIDEO%
set "INPUT_DIR=%VIDEO%%DIRECTORY%"
set "OUTPUT_DIR=%VIDEO%%DIRECTORY%\out"

for %%f in ("%INPUT_DIR%\*.mkv") do (
	@echo .
	@echo ----- %%f %LANG%
	call "%mkvmerge%" -o "%OUTPUT_DIR%\%%~nf_%LANG%.mkv" --audio-tracks %LANG% --subtitle-tracks %LANG% "%%f"
)

pause