@echo off
chcp 65001
SETLOCAL ENABLEDELAYEDEXPANSION

"%JAVA_HOME%\bin\javaw.exe" -Djava.util.Arrays.useLegacyMergeSort=true -jar ImdbSearch.jar -d

