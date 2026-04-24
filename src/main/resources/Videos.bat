@echo off
chcp 65001
SETLOCAL ENABLEDELAYEDEXPANSION

start %HOMEDRIVE%%HOMEPATH%\Documents\NetBeansProjects\ImdbSearch\bin\ImdbSearch.exe %HOMEDRIVE%%HOMEPATH%\Videos\W
timeout 1
start %HOMEDRIVE%%HOMEPATH%\Documents\NetBeansProjects\ImdbSearch\bin\ImdbSearch.exe %HOMEDRIVE%%HOMEPATH%\Videos

