:begin
call build_and_package.cmd

set wd=`pwd`
call run.cmd
pause
cd wd
goto begin