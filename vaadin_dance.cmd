:local_stuff
echo Going to remove project local stuff:
pause
rem let user see what we delete:
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

:global_stuff
echo Going to remove global stuff:
pause
rem let user see what we delete:
@echo on
rm -r -f %USERPROFILE%\.pnpm-debug.log
rm -r -f %USERPROFILE%\.pnpm-state.json
rmdir /S /Q %USERPROFILE%\.vaadin
rmdir /S /Q %USERPROFILE%\.pnpm-store
rmdir /S /Q D:\.pnpm-store
rmdir /S /Q U:\.pnpm-store
rem clear (and preload) default repository:
rm -r -f %USERPROFILE%\.m2\m2repository\*
cp -R %USERPROFILE%\.m2\repo-preload\* %USERPROFILE%\.m2\m2repository
rem clear (and preload) 2nd repository:
rem rm -r -f D:\m2repository\*
rem cp -R %USERPROFILE%\.m2\repo-preload\* D:\m2repository
@echo off
echo global vaadin-generated stuff removed.
echo.
