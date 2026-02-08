@echo off

for %%P in ("W2", "W2\Top Gun", "W2\Dune", "W2\Les Ripoux") DO (
    @echo ------------------------- %HOMEDRIVE%%HOMEPATH%\Videos\%%~P ------------------------------
	cd %HOMEDRIVE%%HOMEPATH%\Documents\NetBeansProjects\ImdbSearch\bin\	
	call ImdbSearch.exe "%HOMEDRIVE%%HOMEPATH%\Videos\%%~P"
	timeout 3
)

rem pause

