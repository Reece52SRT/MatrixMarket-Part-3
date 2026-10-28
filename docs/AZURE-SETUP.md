# Deploying the API to Azure (plain-language steps)

The API already runs on Azure App Service at
`https://matrixmarket-dcf8dfbjeadrdtf6.southafricanorth-01.azurewebsites.net/`.
After uploading the new code, do the steps below **once** in the Azure Portal.

## 1. Publish the new backend
In Visual Studio: right-click **MatrixMarket.Api** > **Publish** > choose the existing Azure App Service > **Publish**.
Then open `<your-site>/api/v1/health` in a browser - you should see `"status": "ok"`.

## 2. App settings (Azure Portal > your App Service > Settings > Environment variables)
| Name | Value | Why |
|---|---|---|
| `Jwt__Key` | any long random text (40+ characters) | Signs the app's login tokens. Keeps the key out of GitHub. |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | the whole contents of the Firebase service-account JSON file (see step 3) | Turns on real-time push notifications. |
| `AZURE_STORAGE_CONNECTION_STRING` | connection string of a Storage Account (see step 4) | Stores listing photos in Azure Blob Storage. |

Click **Apply**; the app restarts. `/api/v1/health` will now show `"pushNotifications": "enabled"` and `"blobStorage": "azure"`.

> Everything still works without steps 3 and 4 - push notifications are skipped (the in-app inbox still fills up) and photos are stored on the App Service's own disk - but the rubric asks for both, so please do them.

## 3. Firebase service account (for push notifications)
Firebase Console > project **matrixmarket-63738** > gear icon > **Project settings** > **Service accounts** > **Generate new private key**. A `.json` file downloads. Open it in Notepad, copy everything, paste it as the value of `FIREBASE_SERVICE_ACCOUNT_JSON`. **Never commit this file to GitHub.**

## 4. Azure Blob Storage (for listing photos)
Azure Portal > **Create a resource** > **Storage account** > same resource group and region (South Africa North) > Standard / LRS > Create.
Open it > **Security + networking** > **Access keys** > copy **Connection string** > paste as `AZURE_STORAGE_CONNECTION_STRING`.
The API creates a private container called `listing-images` by itself and streams photos to the app.

## Where the data lives
| Data | Store | Type |
|---|---|---|
| Users, listings, offers, trade meetings, badges | SQLite via EF Core (`/home/data/matrixmarket/matrixmarket.db`) | Relational |
| Notification inbox, watchlist, push tokens | LiteDB (`matrixmarket-nosql.db`) | NoSQL document DB |
| Listing photos | Azure Blob Storage container `listing-images` | Blob storage |
