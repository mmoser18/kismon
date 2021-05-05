:begin
call build_and_package.cmd
echo "<ENTER> to run application, ^C to quit:"
pause

call run.cmd
pause
goto begin