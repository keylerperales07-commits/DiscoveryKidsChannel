pkg update && pkg install git
COLOR 09
dir
pkg Upgrade
pkg upgrade
git config --global user.name "TuNombre"
git config --global user.email "keylerperales07@gmail.com"
git config --global user.name "keylerperales07-commits"
cd "/storage/emulated/0/AndroidIDEProjects/Discovery Kids Channel"
git init
termux-setup-storage
git init
git branch -m main
git config --global --add safe.directory '/storage/emulated/0/AndroidIDEProjects/Discovery Kids Channel'
git status
git add .
git status
git commit -m "Initial commit"
git status
git branch -m main
git branch
git remote add origin https://github.com/keylerperales07-commits/DiscoveryKidsChannel
git push -u origin main
git pull origin main
git add .
git commit -m "Actualizacion 2.3.0"
git pull origin main
git add --ignore-removal .
git commit -m "Update 2.3.0"
git pull --rebase origin main
git add --ignore-removal .
git pull origin main
git status
git add --ignore-removal .
git rebase --abort
git add --ignore-removal .
git commit -m "Update 2.3.0"
git pull --rebase origin main
git rebase --continue
git add README.md
git rebase --continue
git rebase abort
git rebase --abort
git add release/2000/2.3.0
git commit -m "Update 2.3.0"
exit
