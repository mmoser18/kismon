:begin
cd U:\Documents\eclipse\workspace_Vaadin\KIS-Monitoring
call build_and_package.cmd

set wd=`pwd`
call run.cmd
pause
cd wd
goto begin