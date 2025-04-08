# CI/CD Workflows

This directory contains GitHub Actions workflows for continuous integration and continuous deployment of the EazyDelivery app.

## Workflows

### CI Workflow (`ci.yml`)

The CI workflow runs on every push to the `main` and `develop` branches, as well as on pull requests to these branches. It performs the following tasks:

1. **Build**: Builds the debug version of the app.
2. **Unit Tests**: Runs all unit tests and uploads the results as artifacts.
3. **Lint**: Runs Android Lint and uploads the results as artifacts.
4. **Instrumented Tests**: Runs instrumented tests on an Android emulator.
5. **Code Quality Checks**: Runs Detekt and KtLint to check code quality.

### CD Workflow (`cd.yml`)

The CD workflow runs when a tag with the format `v*` (e.g., `v1.0.0`) is pushed to the repository. It performs the following tasks:

1. **Build**: Builds the release version of the app and creates an APK and an AAB (Android App Bundle).
2. **Deploy to Firebase App Distribution**: Deploys the APK to Firebase App Distribution for testing.
3. **Deploy to Play Store**: Deploys the AAB to the Play Store internal testing track.
4. **Create GitHub Release**: Creates a GitHub release with the APK and AAB attached.

## Secrets

The following secrets need to be set in the GitHub repository settings:

### For Signing the App

- `KEYSTORE_FILE`: Base64-encoded keystore file.
- `KEYSTORE_PASSWORD`: Password for the keystore.
- `KEY_ALIAS`: Alias of the key in the keystore.
- `KEY_PASSWORD`: Password for the key.

### For Firebase App Distribution

- `FIREBASE_APP_ID`: The Firebase App ID.
- `FIREBASE_SERVICE_ACCOUNT`: The Firebase service account JSON file content.

### For Play Store Deployment

- `PLAY_STORE_SERVICE_ACCOUNT_JSON`: The Play Store service account JSON file content.

## Setting Up Secrets

To set up the secrets:

1. Go to your GitHub repository.
2. Click on "Settings" > "Secrets and variables" > "Actions".
3. Click on "New repository secret" and add each of the secrets listed above.

For the `KEYSTORE_FILE` secret, you need to base64-encode the keystore file:

```bash
# On Linux/macOS
base64 keystore.jks

# On Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore.jks"))
```

## Running Workflows Locally

You can use [act](https://github.com/nektos/act) to run the workflows locally:

```bash
# Install act
brew install act

# Run the CI workflow
act -j build

# Run the CD workflow
act -j build --secret-file .secrets
```

Note: You need to create a `.secrets` file with the required secrets for the CD workflow.

## Troubleshooting

If you encounter issues with the workflows:

1. Check the workflow logs in the GitHub Actions tab.
2. Ensure all required secrets are set correctly.
3. Verify that the keystore file is valid and the passwords are correct.
4. Check that the Firebase and Play Store service accounts have the necessary permissions.
