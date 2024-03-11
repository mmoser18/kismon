git filter-repo --path "*/ksta.kmc" --path "*/zhservices.kmc" --path "*/frobi.kmc" --invert-paths --force
git filter-repo --path "ELCA_certificates/*" --invert-paths --force
git filter-repo --path "old_running_version/*" --path "old_running_version" --invert-paths --force
git filter-repo --path "running_version/*.jar" --invert-paths --force
pause
