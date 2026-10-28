# Start here - getting the Final POE onto GitHub and running

1. **Replace the repo contents.** In the GitHub Classroom repo, delete the old files and copy
   everything from this folder in (keep the hidden `.github` and `.gitignore`). Commit and push.
2. **Watch the Actions tab** on GitHub. Both jobs ("Android" and ".NET 8 API") should go green
   in about 10 minutes. If one goes red, open it, copy the red error text and ask for help with it.
3. **Download the APK** from the finished run: Actions > the run > *Artifacts* > `matrixmarket-apks`.
4. **Azure:** publish the new API and add the 3 settings in `docs/AZURE-SETUP.md`.
5. **Firebase:** add the release SHA-1/SHA-256 (in the separate signing-key folder) so Google
   sign-in works in the release APK.
6. **Signed release APK:** add the 4 GitHub secrets from the signing-key folder.
7. **Before submitting:** add screenshots to `docs/screenshots/`, put the new YouTube link and
   your GitHub user/repo name in `README.md`, and add the team's own AI usage declaration.
8. Record the video using `docs/DEMO-VIDEO-CHECKLIST.md`.
