@echo off

for %%P in ("W3") DO (
    @echo ------------------------- %HOMEDRIVE%%HOMEPATH%\Videos\%%~P ------------------------------
	cd %HOMEDRIVE%%HOMEPATH%\Documents\NetBeansProjects\ImdbSearch\bin\	
	call ImdbSearch.exe "%HOMEDRIVE%%HOMEPATH%\Videos\%%~P"
	timeout 3
)

rem pause

