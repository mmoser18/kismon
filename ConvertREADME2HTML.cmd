rem Script to automatically convert README.md to HTML-help file using 
rem remark (https://github.com/remarkjs/remark) and rehype (https://github.com/rehypejs/rehype):

rem Note: "rehype-toc" is suggested in favour of "remark-toc" but it puts the TOC always at the begin
rem of the document which I didn't like. "remark-toc" allowed me to place the TOC where I wanted it 
rem within the document.

rem enable the below to uninstall things again:
rem goto uninstall

rem once this script has run once you can enable the below "goto" to speed up things a bit:
goto skip_install

:install
call npm install --save-dev markdown-extensions
call npm install --save-dev remark remark-cli remark-parse remark-gfm remark-normalize-headings remark-preset-lint-consistent remark-preset-lint-recommended remark-toc remark-usage remark-rehype
call npm install --save-dev rehype rehype-sanitize rehype-slug rehype rehype-autolink-headings rehype-toc rehype-stringify

:skip_install
rem npx remark .\README.md -o .\src\main\resources\META-INF\resources\help\help.html
npm run format

pause
exit /b

rem to uninstall all the node packages installed for the Markdown -> HTML conversion:
:uninstall
call npm uninstall markdown-extensions
call npm uninstall remark remark-cli remark-gfm remark-parse remark-autolink-headings remark-normalize-headings remark-preset-lint-consistent remark-preset-lint-recommended remark-toc remark-usage
call npm uninstall rehype rehype-parse rehype-sanitize rehype-slug rehype-autolink-headings rehype-toc rehype-stringify
