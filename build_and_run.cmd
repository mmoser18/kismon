:begin
cd U:\Documents\eclipse\workspace_Vaadin\KIS-Monitoring
call build_and_package.cmd

rem mimic: set wd=`cd`
FOR /F "tokens=*" %%g IN ('cd') do (SET wd=%%g)

call run.cmd

echo switching back to '%wd%':
cd "%wd%"
cd
pause
goto begin