:begin
call build_and_package.cmd
echo "<ENTER> to run application, ^C to quit:"
pause

set wd=`pwd`
call run.cmd
pause
cd wd
goto begin