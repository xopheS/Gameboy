Please synchronize all your contributions with the GitLab remote repository.

To do so, you can do the following:

For the first time:

1. Install the latest distribution of Git from https://git-scm.com/downloads
2. Start up Git Bash
3. Navigate to your project root folder
4. Initiate a Git local repository with: git init
5. Add the Git remote repo and name it origin with: git remote add origin git@gitlab.com:dave_and_chris/gameboj.git

Every time after working on the project:

6. Add all the files in your project with: git add .
7. Commit your changes (replace with your own commit message) with: git commit -m "Commit message"
8. Pull the Git remote repo with: git pull origin master
9. Solve merge conflicts
10. Push your changes to the Git remote repo with: git push origin master