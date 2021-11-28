@echo off

:package_entries
set fn=package.json
echo Level 1: Going to remove unsupported Vaadin v19+ entries from %fn%:
pause
rem let user see what we do:
@echo on
type %fn%   | findstr /V /C:"@vaadin/application-theme-plugin" > %fn%_1
type %fn%_1 | findstr /V /C:"@vaadin/stats-plugin"             > %fn%_2
type %fn%_2 | findstr /V /C:"@vaadin/theme-live-reload-plugin" > %fn%_3
type %fn%_3 | findstr /V /C:"@vaadin/theme-loader"             > %fn%_4
rem remove an already existing backup - just in case (if there were one the cp below won't work)
rm %fn%~
rem rename back to original and keep a backup:
cp -b -f %fn%_4 %fn%
rem delete the temp. files":
rm %fn%_?
@echo off
echo unsupported Vaadin v19+ entries removed.from %fn%

:local_stuff
echo Level 2: Going to remove project local stuff:
pause
rem let user see what we do:
@echo on
rmdir /S /Q .\node_modules
rmdir /S /Q .\target
rm package.json
rm package-lock.json
rm pnpm-lock.yaml
rm pnpmfile.js
rm tsconfig.json
rm types.d.ts
rm webpack.config.js
rm webpack.generated.js
@echo off
echo project local vaadin-generated stuff removed.

:global_stuff
echo Level 3: Going to remove global stuff: removing local pnpm stuff and emptying m2repository!
pause
rem let user see what we do:
@echo on
rm -r -f %USERPROFILE%\.pnpm-debug.log
rm -r -f %USERPROFILE%\.pnpm-state.json
rmdir /S /Q %USERPROFILE%\.vaadin
rmdir /S /Q %USERPROFILE%\.pnpm-store
rem just in case:
rmdir /S /Q D:\.pnpm-store
rmdir /S /Q U:\.pnpm-store
rem clear (and preload) default repository:
rm -r -f %USERPROFILE%\.m2\m2repository\*
cp -R %USERPROFILE%\.m2\repo-preload\* %USERPROFILE%\.m2\m2repository
rem clear (and preload) 2nd repository:
rem rm -r -f D:\m2repository\*
rem cp -R %USERPROFILE%\.m2\repo-preload\* D:\m2repository
@echo off
echo global vaadin-installed stuff removed.
echo.
pause
